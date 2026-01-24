package com.tiborlaszlo.keywave.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tiborlaszlo.keywave.R

class KeyWaveForegroundService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      // Android 14+ requires specifying the foreground service type
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      // Android 10-13: Use a generic type since SPECIAL_USE wasn't available
      @Suppress("DEPRECATION")
      startForeground(NOTIFICATION_ID, notification)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
    return START_STICKY
  }

  private fun buildNotification(): Notification {
    val manager = getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
      CHANNEL_ID,
      getString(R.string.foreground_channel_name),
      NotificationManager.IMPORTANCE_MIN,
    )
    manager.createNotificationChannel(channel)

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_media_play)
      .setContentTitle(getString(R.string.app_name))
      .setContentText(getString(R.string.foreground_notification_text))
      .setOngoing(true)
      .build()
  }

  companion object {
    private const val CHANNEL_ID = "keywave_foreground"
    private const val NOTIFICATION_ID = 1001
  }
}
