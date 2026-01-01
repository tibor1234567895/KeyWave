package com.tiborlaszlo.keywave.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.tiborlaszlo.keywave.service.KeyWaveAccessibilityService

private const val DELIMITER = ":"

data class PermissionsStatus(
  val accessibilityEnabled: Boolean,
  val notificationListenerEnabled: Boolean,
  val batteryOptimizationIgnored: Boolean,
) {
  val requiredGranted: Boolean
    get() = accessibilityEnabled && notificationListenerEnabled
}

object PermissionsChecker {
  fun check(context: Context): PermissionsStatus {
    return PermissionsStatus(
      accessibilityEnabled = isAccessibilityEnabled(context),
      notificationListenerEnabled = isNotificationListenerEnabled(context),
      batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context),
    )
  }

  fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getInt(
      context.contentResolver,
      Settings.Secure.ACCESSIBILITY_ENABLED,
      0
    )
    if (enabled != 1) return false

    val enabledServices = Settings.Secure.getString(
      context.contentResolver,
      Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val component = android.content.ComponentName(context, KeyWaveAccessibilityService::class.java)
    val flat = component.flattenToString()
    val shortFlat = component.flattenToShortString()
    return enabledServices.split(DELIMITER).any { entry ->
      entry.equals(flat, ignoreCase = true) || entry.equals(shortFlat, ignoreCase = true)
    }
  }

  fun isNotificationListenerEnabled(context: Context): Boolean {
    val packages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packages.contains(context.packageName)
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java)
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
  }
}

object PermissionIntents {
  fun accessibilitySettings(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

  fun notificationListenerSettings(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

  fun batteryOptimizationSettings(context: Context): Intent {
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
      data = Uri.parse("package:${context.packageName}")
    }
    return if (canResolve(context, requestIntent)) {
      requestIntent
    } else {
      val batterySettings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      if (canResolve(context, batterySettings)) {
        batterySettings
      } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.parse("package:${context.packageName}")
        }
      }
    }
  }

  private fun canResolve(context: Context, intent: Intent): Boolean {
    val pm = context.packageManager
    return intent.resolveActivity(pm) != null
  }
}

object PermissionLauncher {
  fun safeStart(context: Context, intent: Intent): Boolean {
    try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      return true
    } catch (_: Exception) {
      return false
    }
  }

  fun safeStartOrToast(context: Context, intent: Intent) {
    val ok = safeStart(context, intent)
    if (!ok) {
      android.widget.Toast.makeText(
        context,
        "No system screen available for this action.",
        android.widget.Toast.LENGTH_SHORT,
      ).show()
    }
  }
}
