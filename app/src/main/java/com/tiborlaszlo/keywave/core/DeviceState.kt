package com.tiborlaszlo.keywave.core

import android.content.Context
import android.os.PowerManager

class DeviceState(context: Context) {
  private val powerManager = context.getSystemService(PowerManager::class.java)

  fun isScreenOn(): Boolean {
    return powerManager.isInteractive
  }
}
