package com.tiborlaszlo.keywave.core

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent

/**
 * Detects volume key gestures (long press on single key or both keys).
 * 
 * KEY BEHAVIOR:
 * - Acquires PARTIAL wakelock on KEY_DOWN to keep CPU awake
 * - Returns TRUE immediately on KEY_DOWN to prevent volume changes
 * - If released before threshold: adjusts volume normally (short press)
 * - If held past threshold: triggers long-press action
 * - Releases wakelock on KEY_UP or after action
 * - Detects missing KEY_UP events (system interception)
 */
class VolumeKeyGestureDetector(
    private val context: Context,
    private val volumeUpThresholdMs: () -> Long,
    private val volumeDownThresholdMs: () -> Long,
    private val comboThresholdMs: () -> Long,
    private val onLongPress: (keyCode: Int) -> Unit,
    private val onBothLongPress: () -> Unit,
    private val onSystemInterceptionDetected: () -> Unit = {},
    private val isDebugEnabled: () -> Boolean = { false },
) {
    companion object {
        private const val TAG = "KeyWaveGesture"
        private const val MISSING_KEY_UP_TIMEOUT_MS = 3000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    // Wakelock to keep CPU awake during gesture detection
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var comboTriggered = false
    
    // Track if we triggered an action for this press cycle
    private var volumeUpActionTriggered = false
    private var volumeDownActionTriggered = false
    
    // Track consecutive missing KEY_UP events for system interception detection
    private var consecutiveMissingKeyUpCount = 0

    private val volumeUpRunnable = Runnable {
        if (!comboTriggered && volumeUpPressed && !volumeDownPressed) {
            Log.w(TAG, ">>> Vol UP long-press threshold reached - triggering action")
            volumeUpActionTriggered = true
            onLongPress(KeyEvent.KEYCODE_VOLUME_UP)
        }
    }

    private val volumeDownRunnable = Runnable {
        if (!comboTriggered && volumeDownPressed && !volumeUpPressed) {
            Log.w(TAG, ">>> Vol DOWN long-press threshold reached - triggering action")
            volumeDownActionTriggered = true
            onLongPress(KeyEvent.KEYCODE_VOLUME_DOWN)
        }
    }

    private val comboRunnable = Runnable {
        if (volumeUpPressed && volumeDownPressed && !comboTriggered) {
            Log.w(TAG, ">>> Combo long-press threshold reached - triggering action")
            comboTriggered = true
            volumeUpActionTriggered = true
            volumeDownActionTriggered = true
            onBothLongPress()
        }
    }
    
    // Runnable to detect missing KEY_UP (system interception)
    private val missingKeyUpRunnable = Runnable {
        if (volumeUpPressed || volumeDownPressed) {
            Log.w(TAG, "!!! Missing KEY_UP detected - possible system interception")
            consecutiveMissingKeyUpCount++
            
            // Reset state
            volumeUpPressed = false
            volumeDownPressed = false
            volumeUpActionTriggered = false
            volumeDownActionTriggered = false
            comboTriggered = false
            releaseWakeLock()
            
            // Notify after 2 consecutive detections
            if (consecutiveMissingKeyUpCount >= 2) {
                onSystemInterceptionDetected()
                consecutiveMissingKeyUpCount = 0
            }
        }
    }

    fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleKeyDown(event, keyCode)
            KeyEvent.ACTION_UP -> handleKeyUp(keyCode)
            else -> false
        }
    }
    
    private fun handleKeyDown(event: KeyEvent, keyCode: Int): Boolean {
        // Ignore repeated key events (from holding the button)
        if (event.repeatCount > 0) {
            Log.w(TAG, "Consuming repeat event for ${keyCodeStr(keyCode)}")
            return true
        }
        
        Log.w(TAG, "Key DOWN: ${keyCodeStr(keyCode)} - CONSUMING to prevent volume change")
        
        // Acquire wakelock to keep CPU awake for long-press detection
        acquireWakeLock()
        
        // Cancel any pending missing KEY_UP detection
        handler.removeCallbacks(missingKeyUpRunnable)
        
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressed = true
            volumeUpActionTriggered = false
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDownPressed = true
            volumeDownActionTriggered = false
        }

        if (volumeUpPressed && volumeDownPressed) {
            // Both pressed - switch to combo mode
            handler.removeCallbacks(volumeUpRunnable)
            handler.removeCallbacks(volumeDownRunnable)
            handler.postDelayed(comboRunnable, comboThresholdMs())
            Log.w(TAG, "Combo detection started, threshold: ${comboThresholdMs()}ms")
        } else {
            // Single key pressed
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                handler.postDelayed(volumeUpRunnable, volumeUpThresholdMs())
                Log.w(TAG, "Vol UP detection started, threshold: ${volumeUpThresholdMs()}ms")
            } else {
                handler.postDelayed(volumeDownRunnable, volumeDownThresholdMs())
                Log.w(TAG, "Vol DOWN detection started, threshold: ${volumeDownThresholdMs()}ms")
            }
        }
        
        // Schedule missing KEY_UP detection
        handler.postDelayed(missingKeyUpRunnable, MISSING_KEY_UP_TIMEOUT_MS)
        
        // CONSUME immediately to prevent volume change
        return true
    }
    
    private fun handleKeyUp(keyCode: Int): Boolean {
        Log.w(TAG, "Key UP: ${keyCodeStr(keyCode)}")
        
        // Cancel missing KEY_UP detection - we got the UP event
        handler.removeCallbacks(missingKeyUpRunnable)
        consecutiveMissingKeyUpCount = 0 // Reset counter on successful KEY_UP
        
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressed = false
            handler.removeCallbacks(volumeUpRunnable)
            if (!volumeUpActionTriggered && !comboTriggered) {
                // Short press - adjust volume up
                Log.w(TAG, "Vol UP short press - adjusting volume UP")
                adjustVolume(AudioManager.ADJUST_RAISE)
            }
            volumeUpActionTriggered = false
        } else {
            volumeDownPressed = false
            handler.removeCallbacks(volumeDownRunnable)
            if (!volumeDownActionTriggered && !comboTriggered) {
                // Short press - adjust volume down
                Log.w(TAG, "Vol DOWN short press - adjusting volume DOWN")
                adjustVolume(AudioManager.ADJUST_LOWER)
            }
            volumeDownActionTriggered = false
        }

        if (!volumeUpPressed || !volumeDownPressed) {
            handler.removeCallbacks(comboRunnable)
        }

        if (!volumeUpPressed && !volumeDownPressed) {
            comboTriggered = false
            // Release wakelock when all keys are released
            releaseWakeLock()
        }
        
        // Always consume UP events for volume keys
        return true
    }
    
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "KeyWave:GestureDetection"
            )
        }
        if (wakeLock?.isHeld == false) {
            // Acquire for max 5 seconds (safety timeout)
            wakeLock?.acquire(5000)
            Log.w(TAG, "WakeLock acquired for gesture detection")
        }
    }
    
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
                Log.w(TAG, "WakeLock released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wakelock: ${e.message}")
            }
        }
    }
    
    private fun adjustVolume(direction: Int) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            Log.w(TAG, "Volume adjusted: ${if (direction == AudioManager.ADJUST_RAISE) "UP" else "DOWN"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume", e)
        }
    }
    
    private fun keyCodeStr(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> "VOL_UP"
        KeyEvent.KEYCODE_VOLUME_DOWN -> "VOL_DOWN"
        else -> "KEY($keyCode)"
    }
}
