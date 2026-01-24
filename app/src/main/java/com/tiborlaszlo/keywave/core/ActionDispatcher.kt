package com.tiborlaszlo.keywave.core

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.session.MediaController
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
            ActionType.NEXT -> sendMediaCommand(settings, KeyEvent.KEYCODE_MEDIA_NEXT) {
                it.transportControls.skipToNext()
            }
            ActionType.PREVIOUS -> sendMediaCommand(settings, KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                it.transportControls.skipToPrevious()
            }
            ActionType.PLAY_PAUSE -> sendMediaCommand(settings, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { controller ->
                val state = controller.playbackState?.state
                if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
            ActionType.STOP -> sendMediaCommand(settings, KeyEvent.KEYCODE_MEDIA_STOP) {
                it.transportControls.stop()
            }
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

    private fun sendMediaCommand(
        settings: SettingsState,
        fallbackKeyCode: Int,
        block: (MediaController) -> Unit
    ) {
        val controller = mediaSessionHelper.getPreferredController(
            activationMode = settings.activationMode,
            allowlist = settings.allowlist,
            blocklist = settings.blocklist,
        )

        if (controller != null) {
            // Plan A: Send direct command to the specific app's media session
            debugLog("Sending command to controller: ${controller.packageName}")
            block(controller)
        } else {
            // Plan B: Fallback to generic media button event (wakes up Dozing apps)
            debugLog("No active media session found, using fallback media key event")
            sendFallbackKeyEvent(fallbackKeyCode)
        }
    }

    private fun sendFallbackKeyEvent(keyCode: Int) {
        try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            debugLog("Dispatched fallback media key event: $keyCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch fallback key event", e)
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
