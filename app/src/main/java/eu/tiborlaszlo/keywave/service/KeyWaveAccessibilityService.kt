package eu.tiborlaszlo.keywave.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.properties.Delegates

class KeyWaveAccessibilityService : AccessibilityService() {
    private val TAG = "KeyWaveAccessibilityService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var vibrator: Vibrator
    private lateinit var settingsManager: SettingsManager
    
    private var volumeUpPressTime by Delegates.notNull<Long>()
    private var volumeDownPressTime by Delegates.notNull<Long>()
    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private var hasTriggeredAction = false
    
    override fun onCreate() {
        super.onCreate()
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            vibrator = getSystemService(Vibrator::class.java)
            settingsManager = SettingsManager(this)
            
            Log.i(TAG, "KeyWave Accessibility Service created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize accessibility service", e)
            disableSelf()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            // Ensure all required services are available
            if (!::audioManager.isInitialized || !::powerManager.isInitialized || 
                !::vibrator.isInitialized || !::settingsManager.isInitialized) {
                Log.e(TAG, "Required services not initialized")
                disableSelf()
                return
            }
            resetAllStates() // Reset states when service connects
            Log.i(TAG, "KeyWave Accessibility Service connected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect accessibility service", e)
            disableSelf()
        }
    }    override fun onDestroy() {
        try {
            (serviceScope.coroutineContext[Job] as? CompletableJob)?.cancel()
            Log.i(TAG, "KeyWave Accessibility Service destroyed")
        } finally {
            super.onDestroy()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle any accessibility events
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        
        try {
            // Verify the event is valid early
            if (event.action != KeyEvent.ACTION_UP && event.action != KeyEvent.ACTION_DOWN) {
                return false
            }
            
            // Add a safety check for required services
            if (!::audioManager.isInitialized || !::powerManager.isInitialized || 
                !::vibrator.isInitialized || !::settingsManager.isInitialized) {
                Log.e(TAG, "Required services not initialized during key event")
                return false
            }
            
            // Check if app is enabled
            var isEnabled = false
            try {
                runBlocking {
                    isEnabled = settingsManager.isAppEnabled.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking app enabled state", e)
                return false
            }
            
            if (!isEnabled) {
                return false
            }
            
            // Only handle events when screen is off
            if (powerManager.isInteractive) {
                return false
            }
            
            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> handleVolumeUpKey(event)
                KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolumeDownKey(event)
                else -> false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing key event", e)
            return false
        }
    }    private fun handleVolumeUpKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {                    // If other button is already pressed, prevent volume change
                    if (isVolumeDownPressed) {
                        isVolumeUpPressed = true
                        volumeUpPressTime = SystemClock.uptimeMillis()
                        Log.d(TAG, "Volume Up pressed during simultaneous press")
                        return true
                    }

                    if (!isVolumeUpPressed && !hasTriggeredAction) {
                        volumeUpPressTime = SystemClock.uptimeMillis()
                        isVolumeUpPressed = true
                        Log.d(TAG, "Volume Up pressed")
                        
                        // Start long press detection
                        serviceScope.launch {
                            try {
                                val threshold = settingsManager.getNextTrackThreshold.first()
                                while (isVolumeUpPressed && !hasTriggeredAction) {
                                    val pressDuration = SystemClock.uptimeMillis() - volumeUpPressTime
                                    
                                    // If volume down is also pressed, check for simultaneous
                                    if (isVolumeDownPressed) {
                                        checkSimultaneousLongPress()
                                        break
                                    }
                                    
                                    // Otherwise check for next track threshold
                                    if (pressDuration >= threshold && !hasTriggeredAction) {
                                        hasTriggeredAction = true
                                        provideHapticFeedback()
                                        skipToNextTrack()
                                        break
                                    }
                                    delay(10)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in volume up monitoring", e)
                            }
                        }
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeUpPressed) {
                        Log.d(TAG, "Volume Up released")
                        isVolumeUpPressed = false
                        
                        if (!hasTriggeredAction) {
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        }
                        
                        // Reset states if both buttons are now released
                        if (!isVolumeDownPressed) {
                            hasTriggeredAction = false
                            Log.d(TAG, "All buttons released, states reset")
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleVolumeUpKey", e)
            isVolumeUpPressed = false
            hasTriggeredAction = false
            return false
        }
    }    private fun handleVolumeDownKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {                    // If other button is already pressed, prevent volume change
                    if (isVolumeUpPressed) {
                        isVolumeDownPressed = true
                        volumeDownPressTime = SystemClock.uptimeMillis()
                        Log.d(TAG, "Volume Down pressed during simultaneous press")
                        return true
                    }

                    if (!isVolumeDownPressed && !hasTriggeredAction) {
                        volumeDownPressTime = SystemClock.uptimeMillis()
                        isVolumeDownPressed = true
                        Log.d(TAG, "Volume Down pressed")
                        
                        // Start long press detection
                        serviceScope.launch {
                            try {
                                val threshold = settingsManager.getPreviousTrackThreshold.first()
                                while (isVolumeDownPressed && !hasTriggeredAction) {
                                    val pressDuration = SystemClock.uptimeMillis() - volumeDownPressTime
                                    
                                    // If volume up is also pressed, check for simultaneous
                                    if (isVolumeUpPressed) {
                                        checkSimultaneousLongPress()
                                        break
                                    }
                                    
                                    // Otherwise check for previous track threshold
                                    if (pressDuration >= threshold && !hasTriggeredAction) {
                                        hasTriggeredAction = true
                                        provideHapticFeedback()
                                        skipToPreviousTrack()
                                        break
                                    }
                                    delay(10)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in volume down monitoring", e)
                            }
                        }
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeDownPressed) {
                        Log.d(TAG, "Volume Down released")
                        isVolumeDownPressed = false
                        
                        if (!hasTriggeredAction) {
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        }
                        
                        // Reset states if both buttons are now released
                        if (!isVolumeUpPressed) {
                            hasTriggeredAction = false
                            Log.d(TAG, "All buttons released, states reset")
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleVolumeDownKey", e)
            isVolumeDownPressed = false
            hasTriggeredAction = false
            return false
        }
    }    private suspend fun checkSimultaneousLongPress() {
        if (!isVolumeUpPressed || !isVolumeDownPressed || hasTriggeredAction) {
            return
        }
        
        try {
            val timeBetweenPresses = Math.abs(volumeUpPressTime - volumeDownPressTime)
            val playPauseThreshold = settingsManager.getPlayPauseThreshold.first()
            val bufferTime = settingsManager.getSimultaneousPressBuffer.first()
            
            if (timeBetweenPresses <= bufferTime) {
                while (isVolumeUpPressed && isVolumeDownPressed && !hasTriggeredAction) {
                    val upDuration = SystemClock.uptimeMillis() - volumeUpPressTime
                    val downDuration = SystemClock.uptimeMillis() - volumeDownPressTime
                    
                    if (upDuration >= playPauseThreshold && downDuration >= playPauseThreshold) {
                        hasTriggeredAction = true
                        provideHapticFeedback()
                        togglePlayPause()
                        break
                    }
                    delay(10)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in simultaneous press check", e)
            hasTriggeredAction = false
        }
    }
      private fun skipToNextTrack() {
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                withContext(Dispatchers.Default) {
                    delay(50)  // Small delay between down and up events
                }
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                provideHapticFeedback()
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to next track", e)
                showError("Failed to skip to next track")
            }
        }
    }
    
    private fun skipToPreviousTrack() {
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                withContext(Dispatchers.Default) {
                    delay(50)  // Small delay between down and up events
                }
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                provideHapticFeedback()
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to previous track", e)
                showError("Failed to skip to previous track")
            }
        }
    }    private fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause called, hasTriggeredSimultaneousPress: $hasTriggeredAction")
        if (!hasTriggeredAction) return
        
        serviceScope.launch {
            try {
                Log.d(TAG, "Sending MEDIA_PLAY_PAUSE key events")
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                withContext(Dispatchers.Default) {
                    delay(50)  // Small delay between down and up events for better compatibility
                }
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                provideHapticFeedback()
                Log.d(TAG, "Successfully sent play/pause media events")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling play/pause", e)
                showError("Failed to toggle play/pause")
            } finally {
                hasTriggeredAction = false
            }
        }
    }
  
      private fun provideHapticFeedback() {
        serviceScope.launch {
            try {
                if (settingsManager.getHapticFeedbackEnabled.first()) {
                    if (::vibrator.isInitialized) {
                        try {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    50,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error providing haptic feedback", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking haptic feedback setting", e)
            }
        }
    }
    
    private fun showError(message: String) {
        try {
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error message: $message", e)
        }
    }

    private fun resetAllStates() {
        Log.d(TAG, "Resetting all states")
        isVolumeUpPressed = false
        isVolumeDownPressed = false
        hasTriggeredAction = false
    }
}
