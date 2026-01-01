package com.tiborlaszlo.keywave.core

import android.media.session.MediaController
import com.tiborlaszlo.keywave.data.ActionType
import com.tiborlaszlo.keywave.data.SettingsState

class ActionDispatcher(
  private val mediaSessionHelper: MediaSessionHelper,
) {
  fun dispatch(action: ActionType, settings: SettingsState) {
    when (action) {
      ActionType.NEXT -> sendMediaCommand(settings) { it.transportControls.skipToNext() }
      ActionType.PREVIOUS -> sendMediaCommand(settings) { it.transportControls.skipToPrevious() }
      ActionType.PLAY_PAUSE -> sendMediaCommand(settings) { controller ->
        val state = controller.playbackState?.state
        if (state == android.media.session.PlaybackState.STATE_PLAYING) {
          controller.transportControls.pause()
        } else {
          controller.transportControls.play()
        }
      }
      ActionType.STOP -> sendMediaCommand(settings) { it.transportControls.stop() }
      ActionType.MUTE,
      ActionType.LAUNCH_APP,
      ActionType.FLASHLIGHT,
      ActionType.ASSISTANT,
      ActionType.SPOTIFY_LIKE,
      ActionType.NONE,
      -> {
        // TODO: implement non-media actions.
      }
    }
  }

  private fun sendMediaCommand(settings: SettingsState, block: (MediaController) -> Unit) {
    val controller = mediaSessionHelper.getPreferredController(
      activationMode = settings.activationMode,
      allowlist = settings.allowlist,
      blocklist = settings.blocklist,
    ) ?: return
    block(controller)
  }
}
