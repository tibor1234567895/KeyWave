package eu.tiborlaszlo.keywave.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaNotificationListenerService : NotificationListenerService() {
    private val tag = "MediaNotificationListener"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _hasActiveMediaNotification = MutableStateFlow(false)
    private val _activeMediaPackages = MutableStateFlow<Set<String>>(emptySet())
    
    companion object {
        private var instance: MediaNotificationListenerService? = null
        
        fun getInstance(): MediaNotificationListenerService? = instance
        
        fun hasActiveMediaNotification(): Boolean {
            return instance?._hasActiveMediaNotification?.value == true
        }
        
        fun getActiveMediaPackages(): Set<String> {
            return instance?._activeMediaPackages?.value ?: emptySet()
        }
        
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(componentName.flattenToString()) == true
        }
    }
    
    val hasActiveMediaNotification: StateFlow<Boolean> = _hasActiveMediaNotification.asStateFlow()
    val activeMediaPackages: StateFlow<Set<String>> = _activeMediaPackages.asStateFlow()
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(tag, "MediaNotificationListenerService created")
        updateMediaNotificationStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.i(tag, "MediaNotificationListenerService destroyed")
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(tag, "NotificationListener connected")
        updateMediaNotificationStatus()
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(tag, "NotificationListener disconnected")
        _hasActiveMediaNotification.value = false
        _activeMediaPackages.value = emptySet()
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.d(tag, "Notification posted from: ${sbn?.packageName}")
        updateMediaNotificationStatus()
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d(tag, "Notification removed from: ${sbn?.packageName}")
        updateMediaNotificationStatus()
    }
    
    private fun updateMediaNotificationStatus() {
        serviceScope.launch {
            try {
                val activeNotifications = activeNotifications ?: return@launch
                val mediaPackages = mutableSetOf<String>()
                
                for (notification in activeNotifications) {
                    if (isMediaNotification(notification)) {
                        mediaPackages.add(notification.packageName)
                        Log.d(tag, "Found media notification from: ${notification.packageName}")
                    }
                }
                
                val hasMedia = mediaPackages.isNotEmpty()
                if (_hasActiveMediaNotification.value != hasMedia) {
                    _hasActiveMediaNotification.value = hasMedia
                    Log.i(tag, "Media notification status changed: $hasMedia")
                }
                
                if (_activeMediaPackages.value != mediaPackages) {
                    _activeMediaPackages.value = mediaPackages
                    Log.i(tag, "Active media packages: $mediaPackages")
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Error updating media notification status", e)
            }
        }
    }
    
    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        try {
            val notification = sbn.notification ?: return false
            
            // Check if notification has media session
            if (notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) {
                Log.d(tag, "Found media session in notification from ${sbn.packageName}")
                return true
            }
            
            // Check if notification has media actions
            if (hasMediaActions(notification)) {
                Log.d(tag, "Found media actions in notification from ${sbn.packageName}")
                return true
            }
            
            // Check if notification is from a known media category
            if (notification.category == Notification.CATEGORY_TRANSPORT) {
                Log.d(tag, "Found transport category notification from ${sbn.packageName}")
                return true
            }
            
            // Check if notification has media-style template
            if (isMediaStyleNotification(notification)) {
                Log.d(tag, "Found media-style notification from ${sbn.packageName}")
                return true
            }
            
            // Check for common media apps by package name as fallback
            if (isKnownMediaApp(sbn.packageName)) {
                Log.d(tag, "Found known media app notification from ${sbn.packageName}")
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            Log.e(tag, "Error checking if notification is media notification", e)
            return false
        }
    }
    
    private fun hasMediaActions(notification: Notification): Boolean {
        val actions = notification.actions ?: return false
        
        for (action in actions) {
            val actionTitle = action.title?.toString()?.lowercase() ?: continue
            
            // Check for common media action keywords
            if (actionTitle.contains("play") || 
                actionTitle.contains("pause") || 
                actionTitle.contains("next") || 
                actionTitle.contains("previous") || 
                actionTitle.contains("skip") ||
                actionTitle.contains("stop")) {
                return true
            }
        }
        
        return false
    }
    
    private fun isMediaStyleNotification(notification: Notification): Boolean {
        try {
            // Check if notification uses MediaStyle template
            val style = notification.extras?.getString(Notification.EXTRA_TEMPLATE)
            return style == "android.app.Notification\$MediaStyle"
        } catch (e: Exception) {
            Log.e(tag, "Error checking notification style", e)
            return false
        }
    }
    
    private fun isKnownMediaApp(packageName: String): Boolean {
        val knownMediaApps = setOf(
            "com.spotify.music",
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.amazon.mp3",
            "com.apple.android.music",
            "com.pandora.android",
            "com.soundcloud.android",
            "deezer.android.app",
            "com.aspiro.tidal",
            "com.clearchannel.iheartradio.controller",
            "com.audible.application",
            "com.google.android.apps.podcasts",
            "com.bambuna.podcastaddict",
            "com.shazam.android",
            "com.last.fm.android",
            "com.maxmpz.audioplayer",
            "com.jetappfactory.jetaudio",
            "com.doubleTwist.androidPlayer",
            "com.neutroncode.mp",
            "com.musicplayer.musicplayer",
            "com.ichi2.anki", // Anki has audio playback
            "com.android.music" // Stock Android music app
        )
        
        return knownMediaApps.contains(packageName)
    }
}
