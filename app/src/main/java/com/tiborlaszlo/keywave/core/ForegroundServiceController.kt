package com.tiborlaszlo.keywave.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tiborlaszlo.keywave.service.KeyWaveForegroundService

object ForegroundServiceController {
  fun start(context: Context) {
    val intent = Intent(context, KeyWaveForegroundService::class.java)
    ContextCompat.startForegroundService(context, intent)
  }

  fun stop(context: Context) {
    val intent = Intent(context, KeyWaveForegroundService::class.java)
    context.stopService(intent)
  }
}
