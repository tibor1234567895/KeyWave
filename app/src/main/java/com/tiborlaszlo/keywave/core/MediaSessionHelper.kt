package com.tiborlaszlo.keywave.core

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.tiborlaszlo.keywave.data.ActivationMode

class MediaSessionHelper(
  context: Context,
  private val listenerComponent: ComponentName,
) {
  private val mediaSessionManager =
    context.getSystemService(MediaSessionManager::class.java)

  fun getActiveSessions(): List<MediaController> {
    return mediaSessionManager.getActiveSessions(listenerComponent)
  }

  fun getPreferredController(
    activationMode: ActivationMode,
    allowlist: Set<String>,
    blocklist: Set<String>,
  ): MediaController? {
    val sessions = getActiveSessions().filter { controller ->
      when {
        allowlist.isNotEmpty() -> allowlist.contains(controller.packageName)
        blocklist.isNotEmpty() -> !blocklist.contains(controller.packageName)
        else -> true
      }
    }

    if (sessions.isEmpty()) return null

    val playing = sessions.firstOrNull {
      it.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    return when (activationMode) {
      ActivationMode.MEDIA_PLAYING -> playing
      ActivationMode.MEDIA_ACTIVE -> playing ?: sessions.first()
      ActivationMode.ALWAYS -> sessions.first()
    }
  }
}
