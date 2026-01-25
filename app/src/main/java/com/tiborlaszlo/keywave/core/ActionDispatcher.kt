package com.tiborlaszlo.keywave.core

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.tiborlaszlo.keywave.data.ActionType
import com.tiborlaszlo.keywave.data.SettingsState

class ActionDispatcher(
    private val context: Context,
    private val mediaSessionHelper: MediaSessionHelper,
    private val isDebugEnabled: () -> Boolean = { false },
) {
    companion object {
        private const val TAG = "ActionDispatcher"
    }

    private fun debugLog(message: String) {
        if (isDebugEnabled()) Log.d(TAG, message)
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    @Volatile private var flashlightOn = false
    private var flashlightCameraId: String? = null

    init {
        // Find the camera with a flashlight
        flashlightCameraId = findFlashlightCameraId()
        
        // Register callback to keep flashlight state in sync with actual hardware state
        // (handles cases where other apps toggle the flashlight)
        flashlightCameraId?.let { cameraId ->
            try {
                cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(id: String, enabled: Boolean) {
                        if (id == cameraId) {
                            flashlightOn = enabled
                            debugLog("Flashlight state synced: $enabled")
                        }
                    }
                }, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register torch callback", e)
            }
        }
    }

    fun dispatch(action: ActionType, settings: SettingsState) {
        debugLog("Dispatching action: $action")
        when (action) {
            ActionType.NEXT -> sendSmartMediaCommand(
                settings = settings,
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
                requiredAction = PlaybackState.ACTION_SKIP_TO_NEXT,
                transportCommand = { it.transportControls.skipToNext() }
            )
            ActionType.PREVIOUS -> sendSmartMediaCommand(
                settings = settings,
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                requiredAction = PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                transportCommand = { it.transportControls.skipToPrevious() }
            )
            ActionType.PLAY_PAUSE -> sendSmartMediaCommand(
                settings = settings,
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                requiredAction = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE,
                transportCommand = { controller ->
                    val state = controller.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
            )
            ActionType.STOP -> sendSmartMediaCommand(
                settings = settings,
                keyCode = KeyEvent.KEYCODE_MEDIA_STOP,
                requiredAction = PlaybackState.ACTION_STOP,
                transportCommand = { it.transportControls.stop() }
            )
            ActionType.FLASHLIGHT -> toggleFlashlight()
            ActionType.ASSISTANT -> launchAssistant()
            ActionType.MUTE -> toggleMute()
            ActionType.LAUNCH_APP,
            ActionType.SPOTIFY_LIKE,
            ActionType.NONE -> {
                Log.d(TAG, "Action $action not implemented or no-op")
            }
        }
    }

    /**
     * Checks if media is available for the given activation mode.
     * Should be called before dispatching media actions to avoid unnecessary fallbacks.
     */
    fun isMediaAvailable(settings: SettingsState): Boolean {
        return mediaSessionHelper.isMediaAvailable(
            activationMode = settings.activationMode,
            allowlist = settings.allowlist,
            blocklist = settings.blocklist,
        )
    }

    /**
     * Smart media command dispatcher with three-tier fallback:
     * 1. If controller supports the action → use transportControls (most reliable)
     * 2. If controller exists but doesn't advertise support → send targeted key event to controller
     * 3. No controller at all → broadcast system-wide key event (wakes up Dozing apps)
     */
    private fun sendSmartMediaCommand(
        settings: SettingsState,
        keyCode: Int,
        requiredAction: Long,
        transportCommand: (MediaController) -> Unit
    ) {
        val controller = mediaSessionHelper.getPreferredController(
            activationMode = settings.activationMode,
            allowlist = settings.allowlist,
            blocklist = settings.blocklist,
        )

        if (controller != null) {
            val actions = controller.playbackState?.actions ?: 0L
            val supportsAction = (actions and requiredAction) != 0L
            
            if (supportsAction) {
                // Tier 1: Controller officially supports this action - use transportControls
                debugLog("Controller ${controller.packageName} supports action, using transportControls")
                transportCommand(controller)
            } else {
                // Tier 2: Controller exists but doesn't advertise support (e.g., Brave browser)
                // Try multiple approaches - some apps respond to different methods
                debugLog("Controller ${controller.packageName} doesn't advertise action support")
                
                // First try: transportControls anyway (some apps still respond)
                debugLog("Tier 2a: Trying transportControls anyway...")
                try {
                    transportCommand(controller)
                } catch (e: Exception) {
                    debugLog("transportControls failed: ${e.message}")
                }
                
                // Second try: targeted key event to this specific controller
                debugLog("Tier 2b: Sending targeted key event to ${controller.packageName}")
                sendTargetedKeyEvent(controller, keyCode)
                
                // Third try: system-wide broadcast (some apps only respond to this)
                debugLog("Tier 2c: Also sending system-wide key event")
                sendSystemKeyEvent(keyCode)
            }
        } else {
            // Tier 3: No controller - broadcast system-wide (wakes up Dozing apps)
            debugLog("No active media session found, using system-wide fallback key event")
            sendSystemKeyEvent(keyCode)
        }
    }

    /**
     * Sends a key event directly to a specific media controller.
     * This is more reliable than transportControls for apps that don't properly
     * advertise their supported actions (like Brave browser).
     */
    private fun sendTargetedKeyEvent(controller: MediaController, keyCode: Int) {
        try {
            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            controller.dispatchMediaButtonEvent(downEvent)
            controller.dispatchMediaButtonEvent(upEvent)
            debugLog("Dispatched targeted key event $keyCode to ${controller.packageName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch targeted key event, falling back to system", e)
            sendSystemKeyEvent(keyCode)
        }
    }

    /**
     * Broadcasts a media key event system-wide via AudioManager.
     * Used when no specific controller is available.
     */
    private fun sendSystemKeyEvent(keyCode: Int) {
        try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            debugLog("Dispatched system-wide key event: $keyCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch system key event", e)
        }
    }

    private fun findFlashlightCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find flashlight camera", e)
            null
        }
    }

    private fun toggleFlashlight() {
        val cameraId = flashlightCameraId
        if (cameraId == null) {
            debugLog("No flashlight camera available")
            return
        }

        val previousState = flashlightOn
        val newState = !previousState
        try {
            cameraManager.setTorchMode(cameraId, newState)
            flashlightOn = newState
            debugLog("Flashlight toggled: $flashlightOn")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            // Restore previous state on failure
            flashlightOn = previousState
        }
    }

    private fun launchAssistant() {
        try {
            // Try Google Assistant first
            val assistantIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (assistantIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(assistantIntent)
                debugLog("Launched voice assistant")
            } else {
                // Fallback to voice search
                val searchIntent = Intent(Intent.ACTION_SEARCH_LONG_PRESS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(searchIntent)
                debugLog("Launched search assistant")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch assistant", e)
        }
    }

    private fun toggleMute() {
        try {
            val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
            if (isMuted) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            } else {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            debugLog("Toggled mute: was=$isMuted, now=${!isMuted}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle mute", e)
        }
    }
}
