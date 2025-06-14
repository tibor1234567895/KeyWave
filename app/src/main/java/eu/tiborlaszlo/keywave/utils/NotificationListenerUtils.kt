package eu.tiborlaszlo.keywave.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import eu.tiborlaszlo.keywave.service.MediaNotificationListenerService

object NotificationListenerUtils {
    
    fun isNotificationListenerEnabled(context: Context): Boolean {
        return MediaNotificationListenerService.isNotificationListenerEnabled(context)
    }
    
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    fun hasActiveMediaNotification(): Boolean {
        return MediaNotificationListenerService.hasActiveMediaNotification()
    }
    
    fun getActiveMediaPackages(): Set<String> {
        return MediaNotificationListenerService.getActiveMediaPackages()
    }
}
