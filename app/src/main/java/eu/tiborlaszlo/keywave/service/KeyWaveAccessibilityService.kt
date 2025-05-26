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
    // Use Delegates.notNull for non-nullable types that are initialized in onCreate/onServiceConnected.
    // However, for times, initializing to 0L is safer to prevent UninitializedPropertyAccessException
    // if accessed before ACTION_DOWN.
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
            // This avoids blocking calls in onKeyEvent.
            serviceScope.launch {
                settingsManager.isAppEnabled
                    .stateIn( // Convert Flow to StateFlow for efficient observation.
                        scope = serviceScope,
                        started = SharingStarted.WhileSubscribed(5000L), // Keep collecting for 5s after last observer.
                        initialValue = true // Assume enabled initially until DataStore loads.
                    )
                    .collect { enabled ->
                        isAppCurrentlyEnabled = enabled
                        Log.d(TAG, "App enabled state updated to: $isAppCurrentlyEnabled")
                        // If the app was just disabled, reset any active key states.
                        if (!enabled && (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction)) {
                            Log.d(TAG, "App disabled, resetting states.")
                            resetAllStates()
                        }
                    }
            }
            Log.i(TAG, "KeyWave Accessibility Service created and observing settings.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize accessibility service in onCreate", e)
            disableSelf() // Disable service if initialization fails.
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            // Double-check initialization, though onCreate should handle it.
            if (!::audioManager.isInitialized || !::powerManager.isInitialized ||
                !::vibrator.isInitialized || !::settingsManager.isInitialized
            ) {
                Log.e(TAG, "Required services not fully initialized in onServiceConnected.")
                disableSelf()
                return
            }
            resetAllStates() // Ensure a clean state when the service (re)connects.
            Log.i(TAG, "KeyWave Accessibility Service connected.")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onServiceConnected", e)
            disableSelf()
        }
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel() // Cancel all coroutines started by this service.
            Log.i(TAG, "KeyWave Accessibility Service destroyed and scope cancelled.")
        } finally {
            super.onDestroy()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This service focuses on key events, so no action needed here.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted. Resetting states.")
        // Interruption might leave the service in an inconsistent state.
        resetAllStates()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false // Ignore null events.

        try {
            // Only process key down and key up actions.
            if (event.action != KeyEvent.ACTION_UP && event.action != KeyEvent.ACTION_DOWN) {
                return false
            }

            // Ensure all required components are initialized.
            if (!::audioManager.isInitialized || !::powerManager.isInitialized ||
                !::vibrator.isInitialized || !::settingsManager.isInitialized
            ) {
                Log.e(TAG, "Skipping key event: Required services not initialized.")
                return false // Don't handle if service isn't ready.
            }

            // Check the cached app enabled state.
            if (!isAppCurrentlyEnabled) {
                // If app is disabled, ensure states are clean and don't process further.
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    resetAllStates()
                }
                return false
            }

            // Only handle key events if the screen is off (device is not interactive).
            if (powerManager.isInteractive) {
                // If screen is on, reset any lingering states from when screen was off.
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    Log.d(TAG, "Screen is on, resetting internal key states.")
                    resetAllStates()
                }
                return false // Let system handle volume keys normally.
            }

            // Process specific volume key events.
            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> handleVolumeUpKey(event)
                KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolumeDownKey(event)
                else -> false // Not a volume key, let system handle it.
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error processing key event", e)
            resetAllStates() // Reset to a known safe state on any unexpected error.
            return true // Consume event even on error to prevent unintended system actions.
        }
    }

    private fun handleVolumeUpKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    // If volume down is already pressed, this is part of a simultaneous press.
                    if (isVolumeDownPressed) {
                        isVolumeUpPressed = true // Mark volume up as pressed.
                        volumeUpPressTime = SystemClock.uptimeMillis()
                        Log.d(TAG, "Volume Up pressed (simultaneous press ongoing).")
                        // The checkSimultaneousLongPress will be triggered by the other key's coroutine.
                        return true // Consume event, prevent system volume change.
                    }

                    // If not already pressed and no action has been triggered yet.
                    if (!isVolumeUpPressed && !hasTriggeredAction) {
                        volumeUpPressTime = SystemClock.uptimeMillis()
                        isVolumeUpPressed = true
                        Log.d(TAG, "Volume Up pressed (monitoring for long press).")

                        // Launch coroutine to monitor for long press or simultaneous press.
                        serviceScope.launch {
                            try {
                                val nextTrackThreshold =
                                    settingsManager.getNextTrackThreshold.first()
                                while (isVolumeUpPressed && !hasTriggeredAction) {
                                    val pressDuration = SystemClock.uptimeMillis() - volumeUpPressTime

                                    if (isVolumeDownPressed) { // Check if other key was pressed.
                                        checkSimultaneousLongPress()
                                        break // Simultaneous check will handle or this loop ends.
                                    }

                                    if (pressDuration >= nextTrackThreshold && !hasTriggeredAction) {
                                        hasTriggeredAction = true
                                        skipToNextTrack() // This action includes haptic feedback.
                                        break
                                    }
                                    delay(10) // Check frequently.
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
                        isVolumeUpPressed = false // Mark as released.

                        if (!hasTriggeredAction) {
                            // If no long-press action was triggered, perform normal volume up.
                            Log.d(TAG, "No action triggered, adjusting volume up.")
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        }

                        // If both keys are now released, reset the action trigger flag.
                        if (!isVolumeDownPressed) {
                            Log.d(TAG, "Both keys released, resetting hasTriggeredAction.")
                            hasTriggeredAction = false
                        }
                    }
                }
            }
            return true // Consume the volume key event.
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleVolumeUpKey", e)
            isVolumeUpPressed = false // Reset specific state on error.
            // Consider if hasTriggeredAction should be reset here depending on context.
            return true // Consume event.
        }
    }

    private fun handleVolumeDownKey(event: KeyEvent): Boolean {
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (isVolumeUpPressed) {
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
                            Log.d(TAG, "Both keys released, resetting hasTriggeredAction.")
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
        // This function is called when one key is already held and the other is pressed.
        // It needs to ensure both are still pressed and no other action has been triggered.
        if (!isVolumeUpPressed || !isVolumeDownPressed || hasTriggeredAction) {
            Log.d(
                TAG,
                "Simultaneous check skipped: VolUp=$isVolumeUpPressed, VolDown=$isVolumeDownPressed, Triggered=$hasTriggeredAction"
            )
            return
        }

        Log.d(TAG, "Checking for simultaneous long press...")
        try {
            // Ensure press times are valid (should be set by ACTION_DOWN handlers).
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

            // Check if keys were pressed close enough in time to be considered "simultaneous".
            if (timeBetweenPresses <= bufferTime) {
                // Monitor while both keys are held and no action has been triggered yet by this path.
                while (isVolumeUpPressed && isVolumeDownPressed && !hasTriggeredAction) {
                    val upDuration = SystemClock.uptimeMillis() - volumeUpPressTime
                    val downDuration = SystemClock.uptimeMillis() - volumeDownPressTime

                    // If both keys have been held long enough for the play/pause action.
                    if (upDuration >= playPauseThreshold && downDuration >= playPauseThreshold) {
                        Log.d(TAG, "Simultaneous threshold met. Triggering play/pause.")
                        hasTriggeredAction =
                            true // Mark that this specific action is being triggered.
                        togglePlayPause() // This action includes haptic feedback and resets hasTriggeredAction in its finally.
                        break // Exit loop as action is triggered.
                    }
                    delay(10) // Re-check shortly.
                }
            } else {
                Log.d(
                    TAG,
                    "Simultaneous press buffer exceeded ($timeBetweenPresses ms > $bufferTime ms). Not a play/pause candidate from this path."
                )
                // If buffer is exceeded, it's not a play/pause. The individual key long press
                // coroutines will continue to monitor for their respective actions if applicable.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in simultaneous press check", e)
            // If an error occurs here, it's crucial not to leave `hasTriggeredAction` in a stuck true state
            // if this path was about to set it. However, togglePlayPause has its own finally.
            // This catch is for errors in fetching settings or the logic before calling togglePlayPause.
        }
    }

    private fun skipToNextTrack() {
        Log.d(TAG, "Action: Skip to Next Track")
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                delay(50) // Brief delay between key down and up for compatibility.
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
        }
    }

    private fun togglePlayPause() {
        Log.d(TAG, "Action: Toggle Play/Pause. Current hasTriggeredAction: $hasTriggeredAction")
        // This function assumes hasTriggeredAction was just set to true by the caller (checkSimultaneousLongPress)
        // to indicate this specific action is intended.
        if (!hasTriggeredAction) {
            Log.w(
                TAG,
                "togglePlayPause called but hasTriggeredAction was false (or reset prematurely). Aborting."
            )
            return // Should not happen if logic flow is correct.
        }

        serviceScope.launch {
            try {
                Log.d(TAG, "Dispatching MEDIA_PLAY_PAUSE")
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                provideHapticFeedback() // Provide feedback for this action.
                Log.i(TAG, "Successfully dispatched MEDIA_PLAY_PAUSE")
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching MEDIA_PLAY_PAUSE", e)
                showError("Failed to toggle play/pause")
            } finally {
                // IMPORTANT: After a play/pause attempt (success or failure),
                // reset hasTriggeredAction. This is because play/pause is a discrete action
                // triggered by both keys. Once attempted, that "simultaneous intent" is consumed.
                // This allows individual key releases to behave normally or other actions if keys are re-pressed.
                Log.d(TAG, "Resetting hasTriggeredAction in togglePlayPause finally block.")
                hasTriggeredAction = false
            }
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
        // Ensure Toast is shown on the main thread.
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
        volumeUpPressTime = 0L // Reset press times.
        volumeDownPressTime = 0L
        hasTriggeredAction = false // Reset the action trigger flag.
    }
}