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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var vibrator: Vibrator
    private lateinit var settingsManager: SettingsManager
    private lateinit var wakeLock: PowerManager.WakeLock
    
    private var volumeUpPressTime by Delegates.notNull<Long>()
    private var volumeDownPressTime by Delegates.notNull<Long>()
    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private var hasTriggeredAction = false
    
    // Track active monitoring jobs
    private var volumeUpJob: Job? = null
    private var volumeDownJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            vibrator = getSystemService(Vibrator::class.java)
            settingsManager = SettingsManager(this)
            
            // Create partial wake lock to keep service active
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "KeyWave::ServiceWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
            
            Log.i(TAG, "KeyWave Accessibility Service created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize accessibility service", e)
            disableSelf()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            if (!::audioManager.isInitialized || !::powerManager.isInitialized || 
                !::vibrator.isInitialized || !::settingsManager.isInitialized) {
                Log.e(TAG, "Required services not initialized")
                disableSelf()
                return
            }
            resetAllStates()
            Log.i(TAG, "KeyWave Accessibility Service connected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect accessibility service", e)
            disableSelf()
        }
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel()
            if (::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
            }
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
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        
        try {
            if (event.action != KeyEvent.ACTION_UP && event.action != KeyEvent.ACTION_DOWN) {
                return false
            }
            
            if (!::audioManager.isInitialized || !::powerManager.isInitialized || 
                !::vibrator.isInitialized || !::settingsManager.isInitialized) {
                Log.e(TAG, "Required services not initialized during key event")
                return false
            }
            
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
            
            if (powerManager.isInteractive) {
                return false
            }
            
            // Acquire wake lock when processing key events
            if (!wakeLock.isHeld) {
                wakeLock.acquire(10000) // Release after 10 seconds max
            }
            
            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> handleVolumeUpKey(event)
                KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolumeDownKey(event)
                else -> false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing key event", e)
            return false
        } finally {
            // Release wake lock if no buttons are pressed
            if (!isVolumeUpPressed && !isVolumeDownPressed && wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun handleVolumeUpKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
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
                        
                        // Cancel any existing monitoring
                        volumeUpJob?.cancel()
                        
                        // Start new monitoring with timeout
                        volumeUpJob = serviceScope.launch {
                            try {
                                val threshold = settingsManager.getNextTrackThreshold.first()
                                withTimeoutOrNull(threshold + 500) { // Add small buffer
                                    while (isVolumeUpPressed && !hasTriggeredAction) {
                                        val pressDuration = SystemClock.uptimeMillis() - volumeUpPressTime
                                        
                                        if (isVolumeDownPressed) {
                                            checkSimultaneousLongPress()
                                            break
                                        }
                                        
                                        if (pressDuration >= threshold && !hasTriggeredAction) {
                                            hasTriggeredAction = true
                                            provideHapticFeedback()
                                            skipToNextTrack()
                                            break
                                        }
                                        delay(50) // Reduced polling frequency
                                    }
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
                        volumeUpJob?.cancel()
                        
                        if (!hasTriggeredAction) {
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        }
                        
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
    }

    private fun handleVolumeDownKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
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
                        
                        // Cancel any existing monitoring
                        volumeDownJob?.cancel()
                        
                        // Start new monitoring with timeout
                        volumeDownJob = serviceScope.launch {
                            try {
                                val threshold = settingsManager.getPreviousTrackThreshold.first()
                                withTimeoutOrNull(threshold + 500) { // Add small buffer
                                    while (isVolumeDownPressed && !hasTriggeredAction) {
                                        val pressDuration = SystemClock.uptimeMillis() - volumeDownPressTime
                                        
                                        if (isVolumeUpPressed) {
                                            checkSimultaneousLongPress()
                                            break
                                        }
                                        
                                        if (pressDuration >= threshold && !hasTriggeredAction) {
                                            hasTriggeredAction = true
                                            provideHapticFeedback()
                                            skipToPreviousTrack()
                                            break
                                        }
                                        delay(50) // Reduced polling frequency
                                    }
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
                        volumeDownJob?.cancel()
                        
                        if (!hasTriggeredAction) {
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        }
                        
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
    }

    private suspend fun checkSimultaneousLongPress() {
        if (!isVolumeUpPressed || !isVolumeDownPressed || hasTriggeredAction) {
            return
        }
        
        try {
            val timeBetweenPresses = Math.abs(volumeUpPressTime - volumeDownPressTime)
            val playPauseThreshold = settingsManager.getPlayPauseThreshold.first()
            val bufferTime = settingsManager.getSimultaneousPressBuffer.first()
            
            if (timeBetweenPresses <= bufferTime) {
                withTimeoutOrNull(playPauseThreshold + 500) { // Add small buffer
                    while (isVolumeUpPressed && isVolumeDownPressed && !hasTriggeredAction) {
                        val upDuration = SystemClock.uptimeMillis() - volumeUpPressTime
                        val downDuration = SystemClock.uptimeMillis() - volumeDownPressTime
                        
                        if (upDuration >= playPauseThreshold && downDuration >= playPauseThreshold) {
                            hasTriggeredAction = true
                            provideHapticFeedback()
                            togglePlayPause()
                            break
                        }
                        delay(50) // Reduced polling frequency
                    }
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
                delay(50)
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
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                provideHapticFeedback()
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to previous track", e)
                showError("Failed to skip to previous track")
            }
        }
    }

    private fun togglePlayPause() {
        if (!hasTriggeredAction) return
        
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                provideHapticFeedback()
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
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error providing haptic feedback", e)
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
        volumeUpJob?.cancel()
        volumeDownJob?.cancel()
    }
}