package eu.tiborlaszlo.keywave.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import eu.tiborlaszlo.keywave.service.KeyWaveAccessibilityService

object AccessibilityUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        val serviceName = context.packageName + "/" + KeyWaveAccessibilityService::class.java.canonicalName
        
        return enabledServices?.split(":")?.any { 
            TextUtils.equals(serviceName, it.trim())
        } ?: false
    }
}
