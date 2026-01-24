package com.tiborlaszlo.keywave.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class KeyWaveNotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "KeyWaveNotif"
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.w(TAG, "=== KeyWave Notification Listener CONNECTED ===")
        // Note: Removed startActivity call - Android 10+ blocks background activity starts
        // and attempting it can destabilize the service
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Placeholder for media session detection.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Placeholder for media session detection cleanup.
    }
}
