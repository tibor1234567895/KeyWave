package com.tiborlaszlo.keywave.service

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class KeyWaveNotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "KeyWaveNotif"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.w(TAG, "=== KeyWave Notification Listener CONNECTED ===")
        
        // Delay slightly to ensure permission is fully registered before bringing app back
        handler.postDelayed({
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                launchIntent?.let { startActivity(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Could not bring app to foreground: ${e.message}")
            }
        }, 300)
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Placeholder for media session detection.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Placeholder for media session detection cleanup.
    }
}
