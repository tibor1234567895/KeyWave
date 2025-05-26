package eu.tiborlaszlo.keywave.service

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyWaveAccessibilityService : AccessibilityService() {
    private val TAG = "KeyWaveAccessibilityService"

    // Use SupervisorJob to ensure that if one child coroutine fails, others are not affected.
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var vibrator: Vibrator
    private lateinit var settingsManager: SettingsManager

    // To store the app enabled state from DataStore, collected in onCreate.
    // Default to true, actual value will be loaded from DataStore.
    private var isAppCurrentlyEnabled = true

    // Variables to track key press states and timings.
    private var volumeUpPressTime: Long = 0L
    private var volumeDownPressTime: Long = 0L
    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private var hasTriggeredAction =
        false // Flag to prevent multiple actions from a single long press sequence.

    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize system services and settings manager.
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            powerManager = getSystemService(POWER_SERVICE) as PowerManager
            vibrator = getSystemService(Vibrator::class.java)
            settingsManager = SettingsManager(this)

            // Launch a coroutine to observe the app enabled state from DataStore.
            serviceScope.launch {
                settingsManager.isAppEnabled
                    .stateIn(
                        scope = serviceScope,
                        started = SharingStarted.WhileSubscribed(5000L),
                        initialValue = true
                    )
                    .collect { enabled ->
                        isAppCurrentlyEnabled = enabled
                        Log.d(TAG, "App enabled state updated to: $isAppCurrentlyEnabled")
                        if (!enabled && (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction)) {
                            Log.d(TAG, "App disabled, resetting states.")
                            resetAllStates()
                        }
                    }
            }
            Log.i(TAG, "KeyWave Accessibility Service created and observing settings.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize accessibility service in onCreate", e)
            disableSelf()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            if (!::audioManager.isInitialized || !::powerManager.isInitialized ||
                !::vibrator.isInitialized || !::settingsManager.isInitialized
            ) {
                Log.e(TAG, "Required services not fully initialized in onServiceConnected.")
                disableSelf()
                return
            }
            resetAllStates()
            Log.i(TAG, "KeyWave Accessibility Service connected.")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onServiceConnected", e)
            disableSelf()
        }
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel()
            Log.i(TAG, "KeyWave Accessibility Service destroyed and scope cancelled.")
        } finally {
            super.onDestroy()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for key event filtering
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted. Resetting states.")
        resetAllStates()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        try {
            if (event.action != KeyEvent.ACTION_UP && event.action != KeyEvent.ACTION_DOWN) {
                return false
            }

            if (!::audioManager.isInitialized || !::powerManager.isInitialized ||
                !::vibrator.isInitialized || !::settingsManager.isInitialized
            ) {
                Log.e(TAG, "Skipping key event: Required services not initialized.")
                return false
            }

            if (!isAppCurrentlyEnabled) {
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    resetAllStates()
                }
                return false
            }

            if (powerManager.isInteractive) {
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    Log.d(TAG, "Screen is on, resetting internal key states.")
                    resetAllStates()
                }
                return false
            }

            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> handleVolumeUpKey(event)
                KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolumeDownKey(event)
                else -> false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error processing key event", e)
            resetAllStates()
            return true // Consume event even on error
        }
    }

    private fun handleVolumeUpKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (isVolumeDownPressed) { // Part of a simultaneous press
                        isVolumeUpPressed = true
                        volumeUpPressTime = SystemClock.uptimeMillis()
                        Log.d(TAG, "Volume Up pressed (simultaneous press ongoing).")
                        // checkSimultaneousLongPress will be initiated by the other key's coroutine or this one if it started first
                        return true
                    }

                    if (!isVolumeUpPressed && !hasTriggeredAction) {
                        volumeUpPressTime = SystemClock.uptimeMillis()
                        isVolumeUpPressed = true
                        Log.d(TAG, "Volume Up pressed (monitoring for long press).")

                        serviceScope.launch {
                            try {
                                val nextTrackThreshold =
                                    settingsManager.getNextTrackThreshold.first()
                                while (isVolumeUpPressed && !hasTriggeredAction) { // Loop while key is held and no action triggered
                                    val pressDuration = SystemClock.uptimeMillis() - volumeUpPressTime

                                    if (isVolumeDownPressed) { // Other key pressed during hold
                                        checkSimultaneousLongPress()
                                        break // Exit this monitor, simultaneous will take over or this action is done
                                    }

                                    if (pressDuration >= nextTrackThreshold && !hasTriggeredAction) {
                                        hasTriggeredAction = true
                                        skipToNextTrack()
                                        break
                                    }
                                    delay(10) // Check frequently
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in volume up long press monitoring", e)
                            }
                        }
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeUpPressed) {
                        Log.d(TAG, "Volume Up released.")
                        isVolumeUpPressed = false

                        if (!hasTriggeredAction) {
                            Log.d(TAG, "No action triggered, adjusting volume up.")
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        }

                        // If both keys are now released, reset the action trigger flag.
                        // This is the correct place to reset after any action or no action.
                        if (!isVolumeDownPressed) {
                            Log.d(
                                TAG,
                                "Both keys released (VolUp last), resetting hasTriggeredAction."
                            )
                            hasTriggeredAction = false
                        }
                    }
                }
            }
            return true // Consume the volume key event.
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleVolumeUpKey", e)
            isVolumeUpPressed = false // Reset specific state on error.
            // If a critical error occurs, consider resetAllStates() if appropriate
            return true // Consume event.
        }
    }

    private fun handleVolumeDownKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (isVolumeUpPressed) { // Part of a simultaneous press
                        isVolumeDownPressed = true
                        volumeDownPressTime = SystemClock.uptimeMillis()
                        Log.d(TAG, "Volume Down pressed (simultaneous press ongoing).")
                        return true
                    }

                    if (!isVolumeDownPressed && !hasTriggeredAction) {
                        volumeDownPressTime = SystemClock.uptimeMillis()
                        isVolumeDownPressed = true
                        Log.d(TAG, "Volume Down pressed (monitoring for long press).")

                        serviceScope.launch {
                            try {
                                val previousTrackThreshold =
                                    settingsManager.getPreviousTrackThreshold.first()
                                while (isVolumeDownPressed && !hasTriggeredAction) {
                                    val pressDuration = SystemClock.uptimeMillis() - volumeDownPressTime

                                    if (isVolumeUpPressed) {
                                        checkSimultaneousLongPress()
                                        break
                                    }

                                    if (pressDuration >= previousTrackThreshold && !hasTriggeredAction) {
                                        hasTriggeredAction = true
                                        skipToPreviousTrack()
                                        break
                                    }
                                    delay(10)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in volume down long press monitoring", e)
                            }
                        }
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeDownPressed) {
                        Log.d(TAG, "Volume Down released.")
                        isVolumeDownPressed = false

                        if (!hasTriggeredAction) {
                            Log.d(TAG, "No action triggered, adjusting volume down.")
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        }

                        if (!isVolumeUpPressed) {
                            Log.d(
                                TAG,
                                "Both keys released (VolDown last), resetting hasTriggeredAction."
                            )
                            hasTriggeredAction = false
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleVolumeDownKey", e)
            isVolumeDownPressed = false
            return true
        }
    }

    private suspend fun checkSimultaneousLongPress() {
        if (!isVolumeUpPressed || !isVolumeDownPressed || hasTriggeredAction) {
            Log.d(
                TAG,
                "Simultaneous check skipped: VolUpPresent=$isVolumeUpPressed, VolDownPresent=$isVolumeDownPressed, ActionTriggered=$hasTriggeredAction"
            )
            return
        }

        Log.d(TAG, "Checking for simultaneous long press...")
        try {
            if (volumeUpPressTime == 0L || volumeDownPressTime == 0L) {
                Log.w(TAG, "Simultaneous check aborted: press times not properly set.")
                return
            }

            val timeBetweenPresses = Math.abs(volumeUpPressTime - volumeDownPressTime)
            val playPauseThreshold = settingsManager.getPlayPauseThreshold.first()
            val bufferTime = settingsManager.getSimultaneousPressBuffer.first()

            Log.d(
                TAG,
                "Simultaneous Params: TimeBetween=$timeBetweenPresses (Buffer=$bufferTime), Threshold=$playPauseThreshold"
            )

            if (timeBetweenPresses <= bufferTime) {
                // Both keys are pressed and within the buffer time.
                // Now, wait for both to be held for the playPauseThreshold.
                while (isVolumeUpPressed && isVolumeDownPressed && !hasTriggeredAction) {
                    // We need to check duration from the *later* of the two presses,
                    // or ensure both individual durations meet the threshold.
                    // Simpler: check if current time minus each press time is >= threshold.
                    val upDuration = SystemClock.uptimeMillis() - volumeUpPressTime
                    val downDuration = SystemClock.uptimeMillis() - volumeDownPressTime

                    if (upDuration >= playPauseThreshold && downDuration >= playPauseThreshold) {
                        Log.d(TAG, "Simultaneous threshold met. Triggering play/pause.")
                        hasTriggeredAction = true // Mark that a KeyWave action is being performed.
                        togglePlayPause()
                        // After togglePlayPause, the 'hasTriggeredAction' will remain true
                        // until both keys are released (handled in ACTION_UP).
                        break
                    }
                    delay(10)
                }
            } else {
                Log.d(
                    TAG,
                    "Simultaneous press buffer exceeded ($timeBetweenPresses ms > $bufferTime ms). Not a play/pause."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in simultaneous press check", e)
            // If an error occurs here, reset to be safe, as state might be inconsistent.
            // resetAllStates() // Or specific error handling.
        }
    }

    private fun skipToNextTrack() {
        Log.d(TAG, "Action: Skip to Next Track")
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                provideHapticFeedback()
                Log.i(TAG, "Successfully dispatched MEDIA_NEXT")
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching MEDIA_NEXT", e)
                showError("Failed to skip to next track")
            }
            // hasTriggeredAction is reset when both keys are released.
        }
    }

    private fun skipToPreviousTrack() {
        Log.d(TAG, "Action: Skip to Previous Track")
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                provideHapticFeedback()
                Log.i(TAG, "Successfully dispatched MEDIA_PREVIOUS")
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching MEDIA_PREVIOUS", e)
                showError("Failed to skip to previous track")
            }
            // hasTriggeredAction is reset when both keys are released.
        }
    }

    private fun togglePlayPause() {
        // This function is called when a simultaneous long press is confirmed
        // and hasTriggeredAction has just been set to true by the caller.
        Log.d(TAG, "Action: Toggle Play/Pause. Current hasTriggeredAction: $hasTriggeredAction")
        if (!hasTriggeredAction) { // Should ideally not happen if called correctly
            Log.w(TAG, "togglePlayPause called but hasTriggeredAction was false. Aborting.")
            return
        }

        serviceScope.launch {
            try {
                Log.d(TAG, "Dispatching MEDIA_PLAY_PAUSE")
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                provideHapticFeedback()
                Log.i(TAG, "Successfully dispatched MEDIA_PLAY_PAUSE")
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching MEDIA_PLAY_PAUSE", e)
                showError("Failed to toggle play/pause")
                // If dispatch fails, we might still want hasTriggeredAction to be true
                // until keys are released, to prevent volume changes.
                // Or, if the action is considered "failed", reset hasTriggeredAction here
                // and allow volume changes. Current logic: it stays true until keys up.
            }
            // CRITICAL FIX: Removed 'finally { hasTriggeredAction = false }' block.
            // 'hasTriggeredAction' is reset when both keys are released,
            // handled in ACTION_UP of handleVolumeUpKey/handleVolumeDownKey.
        }
    }

    private fun provideHapticFeedback() {
        serviceScope.launch {
            try {
                if (settingsManager.getHapticFeedbackEnabled.first()) {
                    if (::vibrator.isInitialized && vibrator.hasVibrator()) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        Log.w(TAG, "Vibrator not available or not initialized for haptic feedback.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error providing haptic feedback", e)
            }
        }
    }

    private fun showError(message: String) {
        serviceScope.launch(Dispatchers.Main.immediate) {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing Toast message: '$message'", e)
            }
        }
    }

    private fun resetAllStates() {
        Log.d(TAG, "Resetting all internal key press states.")
        isVolumeUpPressed = false
        isVolumeDownPressed = false
        volumeUpPressTime = 0L
        volumeDownPressTime = 0L
        hasTriggeredAction = false
    }
}
