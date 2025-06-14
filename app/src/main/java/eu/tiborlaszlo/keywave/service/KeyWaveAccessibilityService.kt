package eu.tiborlaszlo.keywave.service

// Unused import: android.content.Context (Removed as 'this' or 'applicationContext' is used)
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import eu.tiborlaszlo.keywave.MainActivity
import eu.tiborlaszlo.keywave.R
import eu.tiborlaszlo.keywave.utils.MediaAction
import eu.tiborlaszlo.keywave.utils.NotificationListenerUtils
import eu.tiborlaszlo.keywave.utils.VibrationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class KeyWaveAccessibilityService : AccessibilityService() {
    private val tag = "KeyWaveAccessibilityService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var vibrator: Vibrator
    private lateinit var settingsManager: SettingsManager
    private lateinit var vibrationManager: VibrationManager

    private var isAppCurrentlyEnabled = true
    private var canVibrate: Boolean = false

    // This is initialized with a default that is always false, which is intended.
    // The actual value is collected from DataStore in onCreate.
    private var isDebugMonitoringEnabled: Boolean = SettingsManager.DEFAULT_DEBUG_MONITORING_ENABLED

    private var volumeUpPressTime: Long = 0L
    private var volumeDownPressTime: Long = 0L
    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private var hasTriggeredAction = false

    private var volumeUpActionTimeoutJob: Job? = null
    private var volumeDownActionTimeoutJob: Job? = null
    private var simultaneousActionTimeoutJob: Job? = null

    // Renamed private vals to camelCase
    private val actionResetTimeoutMs = 7000L
    private val foregroundNotificationId = 1895
    private val notificationChannelId = "KeyWaveServiceChannel"

    // Logging Throttling in Loops
    private val debugLoopLogIntervalMs = 1000L // Log about once per second in loops
    private var lastVolumeUpLogTime: Long = 0L
    private var lastVolumeDownLogTime: Long = 0L
    private var lastSimultaneousLogTime: Long = 0L


    private fun logDebug(message: String) {
        if (isDebugMonitoringEnabled) Log.d(tag, "[DEBUG] $message")
    }

    private fun logInfo(message: String) {
        Log.i(tag, message)
    }

    private fun logWarn(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }

    private fun createNotificationChannel() {
        // SDK_INT check for O (API 26) is unnecessary as minSdk is 26.
        val serviceChannel = NotificationChannel(
            notificationChannelId,
            getString(R.string.keywave_service_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.keywave_service_notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        logInfo("Notification channel created.")
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        // SDK_INT check for M (API 23) is unnecessary as minSdk is 26.
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)
        val notificationIcon =
            R.mipmap.ic_launcher // IMPORTANT: REPLACE with a proper small status bar icon (e.g., R.drawable.ic_stat_keywave)

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(getString(R.string.keywave_service_notification_title))
            .setContentText(getString(R.string.keywave_service_notification_text))
            .setSmallIcon(notificationIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        logInfo("onCreate: Service creating.")
        try {
            createNotificationChannel()
            val notification = buildNotification()
            startForeground(foregroundNotificationId, notification)
            logInfo("Service started in foreground.")

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            powerManager = getSystemService(POWER_SERVICE) as PowerManager
            vibrator = getSystemService(Vibrator::class.java)
            settingsManager = SettingsManager(this)
            vibrationManager = VibrationManager(this)
            canVibrate = ::vibrator.isInitialized && vibrator.hasVibrator()
            if (!canVibrate) logWarn("Vibrator not available or not initialized during onCreate.")

            serviceScope.launch {
                settingsManager.isAppEnabled
                    .stateIn(
                        scope = serviceScope,
                        started = SharingStarted.WhileSubscribed(5000L),
                        initialValue = true
                    )
                    .collect { enabled ->
                        isAppCurrentlyEnabled = enabled
                        logInfo("App enabled state updated to: $isAppCurrentlyEnabled")
                        if (!enabled && (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction)) {
                            logDebug("App disabled by settings, resetting states.")
                            resetAllStates()
                        }
                    }
            }
            serviceScope.launch {
                // The initialValue uses a constant that is always false, which is the intended default.
                // The actual value is collected from DataStore.
                settingsManager.getDebugMonitoringEnabled
                    .stateIn(
                        scope = serviceScope,
                        started = SharingStarted.WhileSubscribed(5000L),
                        initialValue = SettingsManager.DEFAULT_DEBUG_MONITORING_ENABLED
                    )
                    .collect { enabled ->
                        isDebugMonitoringEnabled = enabled
                        logInfo("Debug monitoring state updated to: $isDebugMonitoringEnabled")
                    }
            }
            logInfo("KeyWave Accessibility Service fully created and observing settings.")
        } catch (e: Exception) {
            logError("Failed to initialize accessibility service in onCreate", e); disableSelf()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        logInfo("onServiceConnected: Service connected and bound.")
        try {
            this.serviceInfo = this.serviceInfo?.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            }
            logInfo(
                "ServiceInfo flags updated. Current flags (binary): ${
                    this.serviceInfo?.flags?.toString(
                        2
                    )
                }"
            )
            logInfo("ServiceInfo eventTypes (binary): ${this.serviceInfo?.eventTypes?.toString(2)}")

            if (!::audioManager.isInitialized || !::powerManager.isInitialized || !::vibrator.isInitialized || !::settingsManager.isInitialized) {
                logError("Required services not fully initialized in onServiceConnected."); disableSelf(); return
            }
            canVibrate = ::vibrator.isInitialized && vibrator.hasVibrator()
            if (!canVibrate) logWarn("Vibrator check during onServiceConnected: Not available.")
            logDebug("Resetting states due to onServiceConnected.")
            resetAllStates()
            logInfo("KeyWave Accessibility Service initialization complete after connection.")
        } catch (e: Exception) {
            logError("Error in onServiceConnected", e); disableSelf()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        logWarn("onUnbind: Service is being unbound. HasTriggeredAction: $hasTriggeredAction")
        stopForegroundServiceSafely()
        resetAllStatesAndJobs()
        try {
            serviceScope.cancel("Service onUnbind called"); logInfo("onUnbind: serviceScope cancelled.")
        } catch (e: Exception) {
            logError("onUnbind: Exception during scope cancellation.", e)
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        logWarn("onDestroy: Service attempting to destroy. HasTriggeredAction: $hasTriggeredAction")
        try {
            stopForegroundServiceSafely()
            resetAllStatesAndJobs()
            serviceScope.cancel("Service onDestroy called")
            logInfo("onDestroy: serviceScope cancelled.")
        } catch (e: Exception) {
            logError("onDestroy: Exception during cleanup.", e)
        } finally {
            logWarn("onDestroy: super.onDestroy() being called."); super.onDestroy(); logWarn("onDestroy: Destruction completed.")
        }
    }

    private fun stopForegroundServiceSafely() {
        try {
            // SDK_INT check for N (API 24) is unnecessary as minSdk is 26.
            stopForeground(STOP_FOREGROUND_REMOVE)
            logInfo("Service stopped from foreground.")
        } catch (e: Exception) {
            logError("Error stopping foreground service", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        logDebug("onAccessibilityEvent: ${event?.eventType}")
    }

    override fun onInterrupt() {
        logWarn("onInterrupt: Service interrupted. HasTriggeredAction: $hasTriggeredAction"); resetAllStatesAndJobs()
    }

    private fun cancelAllActionTimeouts() {
        logDebug("Cancelling all action timeouts.")
        volumeUpActionTimeoutJob?.cancel("New event or state reset")
        volumeUpActionTimeoutJob = null
        volumeDownActionTimeoutJob?.cancel("New event or state reset")
        volumeDownActionTimeoutJob = null
        simultaneousActionTimeoutJob?.cancel("New event or state reset")
        simultaneousActionTimeoutJob = null
    }

    private fun startVolumeUpActionTimeout() {
        cancelAllActionTimeouts()
        volumeUpActionTimeoutJob = serviceScope.launch {
            delay(actionResetTimeoutMs)
            if (isVolumeUpPressed && hasTriggeredAction) {
                logWarn("Volume Up action timeout reached! Stuck state detected. Forcing reset.")
                resetAllStates()
            }
        }
        logDebug("Started Volume Up action timeout job: $volumeUpActionTimeoutJob")
    }

    private fun startVolumeDownActionTimeout() {
        cancelAllActionTimeouts()
        volumeDownActionTimeoutJob = serviceScope.launch {
            delay(actionResetTimeoutMs)
            if (isVolumeDownPressed && hasTriggeredAction) {
                logWarn("Volume Down action timeout reached! Stuck state detected. Forcing reset.")
                resetAllStates()
            }
        }
        logDebug("Started Volume Down action timeout job: $volumeDownActionTimeoutJob")
    }

    private fun startSimultaneousActionTimeout() {
        cancelAllActionTimeouts()
        simultaneousActionTimeoutJob = serviceScope.launch {
            delay(actionResetTimeoutMs)
            if (isVolumeUpPressed && isVolumeDownPressed && hasTriggeredAction) {
                logWarn("Simultaneous action timeout reached! Stuck state detected. Forcing reset.")
                resetAllStates()
            }
        }
        logDebug("Started Simultaneous action timeout job: $simultaneousActionTimeoutJob")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) {
            logDebug("onKeyEvent: event is null"); return false
        }
        logDebug("onKeyEvent: action=${event.action}, keyCode=${event.keyCode}, AppEn=$isAppCurrentlyEnabled, ScreenInt=${if (::powerManager.isInitialized) powerManager.isInteractive else "N/A"}, TrigAct=$hasTriggeredAction, MediaActive=${NotificationListenerUtils.hasActiveMediaNotification()}")

        try {
            if (event.action != KeyEvent.ACTION_UP && event.action != KeyEvent.ACTION_DOWN) {
                logDebug("onKeyEvent: Ignoring event action ${event.action}"); return false
            }
            if (!::audioManager.isInitialized || !::powerManager.isInitialized || !::vibrator.isInitialized || !::settingsManager.isInitialized) {
                logError("Skipping key event: Services not initialized."); return false
            }
            if (!isAppCurrentlyEnabled) {
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    logDebug("onKeyEvent: App disabled, resetting."); resetAllStatesAndJobs()                }
                return false
            }
            
            if (powerManager.isInteractive) {
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    logDebug("onKeyEvent: Screen ON, resetting."); resetAllStatesAndJobs()
                }
                return false
            }

            // Check if there's an active media notification
            val hasActiveMedia = NotificationListenerUtils.hasActiveMediaNotification()
            val activeMediaPackages = NotificationListenerUtils.getActiveMediaPackages()
            
            if (!hasActiveMedia) {
                logDebug("onKeyEvent: No active media notification, allowing normal volume behavior. Active packages: $activeMediaPackages")
                if (isVolumeUpPressed || isVolumeDownPressed || hasTriggeredAction) {
                    logDebug("onKeyEvent: Resetting states due to no media notification"); resetAllStatesAndJobs()
                }
                return false
            }
            
            logDebug("onKeyEvent: Media notification active from: $activeMediaPackages")

            logDebug("onKeyEvent: Processing (Screen OFF, App ON, Media Active)")
            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> handleVolumeUpKey(event)
                KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolumeDownKey(event)
                else -> {
                    logDebug("onKeyEvent: Unhandled keyCode ${event.keyCode}"); false
                }
            }
        } catch (e: Exception) {
            logError("Critical error in onKeyEvent", e); resetAllStatesAndJobs(); return true
        }
    }

    @Suppress("SameReturnValue")
    private fun handleVolumeUpKey(event: KeyEvent): Boolean {
        logDebug("handleVolUpKey: act=${event.action}, VUp=$isVolumeUpPressed, VDown=$isVolumeDownPressed, TrigAct=$hasTriggeredAction")
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (isVolumeDownPressed) {
                        isVolumeUpPressed = true; volumeUpPressTime = SystemClock.uptimeMillis()
                        logDebug("VolUp DOWN (simul ongoing). Time: $volumeUpPressTime"); return true
                    }
                    if (!isVolumeUpPressed && !hasTriggeredAction) {
                        volumeUpPressTime = SystemClock.uptimeMillis(); isVolumeUpPressed =
                            true; lastVolumeUpLogTime = 0L
                        logDebug("VolUp DOWN (monitoring). Time: $volumeUpPressTime")
                        serviceScope.launch {
                            try {
                                val threshold = settingsManager.getNextTrackThreshold.first()
                                logDebug("MonitorVolUp: thresh=$threshold")
                                while (isVolumeUpPressed && !hasTriggeredAction) {
                                    val currentTime = SystemClock.uptimeMillis()
                                    val duration = currentTime - volumeUpPressTime
                                    if (isDebugMonitoringEnabled && (currentTime - lastVolumeUpLogTime >= debugLoopLogIntervalMs || lastVolumeUpLogTime == 0L)) {
                                        logDebug("MonitorVolUp: dur=$duration, VDown=$isVolumeDownPressed")
                                        lastVolumeUpLogTime = currentTime
                                    }
                                    if (isVolumeDownPressed) {
                                        logDebug("MonitorVolUp: VDown detected, checking simul."); checkSimultaneousLongPress(); break
                                    }
                                    if (duration >= threshold && !hasTriggeredAction) {
                                        logInfo("VolUp threshold met. Setting TrigAct=true.")
                                        hasTriggeredAction =
                                            true; skipToNextTrack(); startVolumeUpActionTimeout(); break
                                    }
                                    delay(10)
                                }
                                logDebug("MonitorVolUp loop end. VUp=$isVolumeUpPressed, TrigAct=$hasTriggeredAction")
                            } catch (e: Exception) {
                                logError("Err MonitorVolUp", e)
                            }
                        }
                    } else {
                        logDebug("VolUp DOWN: No new monitor. VUp=$isVolumeUpPressed, TrigAct=$hasTriggeredAction")
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeUpPressed) {
                        logDebug("VolUp UP. Dur=${SystemClock.uptimeMillis() - volumeUpPressTime}ms. VDown=$isVolumeDownPressed, TrigAct=$hasTriggeredAction")
                        isVolumeUpPressed = false
                        cancelAllActionTimeouts()
                        if (!hasTriggeredAction) {
                            logDebug("VolUp UP: No action, adjust vol.")
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        }
                        if (!isVolumeDownPressed) {
                            logDebug("VolUp UP: VDown also up. TrigAct was $hasTriggeredAction. Setting false.")
                            hasTriggeredAction = false
                        } else {
                            logDebug("VolUp UP: VDown still pressed. TrigAct remains $hasTriggeredAction")
                        }
                    } else {
                        logDebug("VolUp UP: was not pressed.")
                    }
                }
            }
            return true
        } catch (e: Exception) {
            logError("Err handleVolUpKey", e); isVolumeUpPressed = false; return true
        }
    }

    @Suppress("SameReturnValue")
    private fun handleVolumeDownKey(event: KeyEvent): Boolean {
        logDebug("handleVolDownKey: act=${event.action}, VUp=$isVolumeUpPressed, VDown=$isVolumeDownPressed, TrigAct=$hasTriggeredAction")
        try {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (isVolumeUpPressed) {
                        isVolumeDownPressed = true; volumeDownPressTime = SystemClock.uptimeMillis()
                        logDebug("VolDown DOWN (simul ongoing). Time: $volumeDownPressTime"); return true
                    }
                    if (!isVolumeDownPressed && !hasTriggeredAction) {
                        volumeDownPressTime = SystemClock.uptimeMillis(); isVolumeDownPressed =
                            true; lastVolumeDownLogTime = 0L
                        logDebug("VolDown DOWN (monitoring). Time: $volumeDownPressTime")
                        serviceScope.launch {
                            try {
                                val threshold = settingsManager.getPreviousTrackThreshold.first()
                                logDebug("MonitorVolDown: thresh=$threshold")
                                while (isVolumeDownPressed && !hasTriggeredAction) {
                                    val currentTime = SystemClock.uptimeMillis()
                                    val duration = currentTime - volumeDownPressTime
                                    if (isDebugMonitoringEnabled && (currentTime - lastVolumeDownLogTime >= debugLoopLogIntervalMs || lastVolumeDownLogTime == 0L)) {
                                        logDebug("MonitorVolDown: dur=$duration, VUp=$isVolumeUpPressed")
                                        lastVolumeDownLogTime = currentTime
                                    }
                                    if (isVolumeUpPressed) {
                                        logDebug("MonitorVolDown: VUp detected, checking simul."); checkSimultaneousLongPress(); break
                                    }
                                    if (duration >= threshold && !hasTriggeredAction) {
                                        logInfo("VolDown threshold met. Setting TrigAct=true.")
                                        hasTriggeredAction =
                                            true; skipToPreviousTrack(); startVolumeDownActionTimeout(); break
                                    }
                                    delay(10)
                                }
                                logDebug("MonitorVolDown loop end. VDown=$isVolumeDownPressed, TrigAct=$hasTriggeredAction")
                            } catch (e: Exception) {
                                logError("Err MonitorVolDown", e)
                            }
                        }
                    } else {
                        logDebug("VolDown DOWN: No new monitor. VDown=$isVolumeDownPressed, TrigAct=$hasTriggeredAction")
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isVolumeDownPressed) {
                        logDebug("VolDown UP. Dur=${SystemClock.uptimeMillis() - volumeDownPressTime}ms. VUp=$isVolumeUpPressed, TrigAct=$hasTriggeredAction")
                        isVolumeDownPressed = false
                        cancelAllActionTimeouts()
                        if (!hasTriggeredAction) {
                            logDebug("VolDown UP: No action, adjust vol.")
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        }
                        if (!isVolumeUpPressed) {
                            logDebug("VolDown UP: VUp also up. TrigAct was $hasTriggeredAction. Setting false.")
                            hasTriggeredAction = false
                        } else {
                            logDebug("VolDown UP: VUp still pressed. TrigAct remains $hasTriggeredAction")
                        }
                    } else {
                        logDebug("VolDown UP: was not pressed.")
                    }
                }
            }
            return true
        } catch (e: Exception) {
            logError("Err handleVolDownKey", e); isVolumeDownPressed = false; return true
        }
    }

    private suspend fun checkSimultaneousLongPress() {
        if (!isVolumeUpPressed || !isVolumeDownPressed || hasTriggeredAction) {
            logDebug("Simul check skip: VUp=$isVolumeUpPressed,VDown=$isVolumeDownPressed,TrigAct=$hasTriggeredAction"); return
        }
        logDebug("Checking simul long press... VUpT=$volumeUpPressTime, VDownT=$volumeDownPressTime"); lastSimultaneousLogTime =
            0L
        try {
            if (volumeUpPressTime == 0L || volumeDownPressTime == 0L) {
                logWarn("Simul check: Press times invalid."); return
            }
            val timeBetween = abs(volumeUpPressTime - volumeDownPressTime)
            val threshold = settingsManager.getPlayPauseThreshold.first()
            val buffer = settingsManager.getSimultaneousPressBuffer.first()
            logDebug("Simul Params: Between=$timeBetween (Buf=$buffer), Thresh=$threshold")

            if (timeBetween <= buffer) {
                while (isVolumeUpPressed && isVolumeDownPressed && !hasTriggeredAction) {
                    val currentTime = SystemClock.uptimeMillis()
                    val upDur = currentTime - volumeUpPressTime
                    val downDur = currentTime - volumeDownPressTime
                    if (isDebugMonitoringEnabled && (currentTime - lastSimultaneousLogTime >= debugLoopLogIntervalMs || lastSimultaneousLogTime == 0L)) {
                        logDebug("MonitorSimul: UpD=$upDur, DownD=$downDur")
                        lastSimultaneousLogTime = currentTime
                    }
                    if (upDur >= threshold && downDur >= threshold) {
                        logInfo("Simul threshold met. Setting TrigAct=true.")
                        hasTriggeredAction =
                            true; togglePlayPause(); startSimultaneousActionTimeout(); break
                    }
                    delay(10)
                }
                logDebug("MonitorSimul loop end. VUp=$isVolumeUpPressed,VDown=$isVolumeDownPressed,TrigAct=$hasTriggeredAction")
            } else {
                logDebug("Simul buffer exceeded ($timeBetween > $buffer).")
            }
        } catch (e: Exception) {
            logError("Err checkSimulPress", e)
        }
    }    private fun skipToNextTrack() {
        logInfo("Action: SkipNext (TrigAct is now true)")
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                provideHapticFeedback(MediaAction.NEXT_TRACK)
                logInfo("Dispatch MEDIA_NEXT OK")
            } catch (e: Exception) {
                logError("Err dispatch NEXT", e)
                showError("Fail skip next")
            }
        }
    }    private fun skipToPreviousTrack() {
        logInfo("Action: SkipPrev (TrigAct is now true)")
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                provideHapticFeedback(MediaAction.PREVIOUS_TRACK)
                logInfo("Dispatch MEDIA_PREVIOUS OK")
            } catch (e: Exception) {
                logError("Err dispatch PREV", e)
                showError("Fail skip prev")
            }
        }
    }    private fun togglePlayPause() {
        logInfo("Action: TogglePlayPause (TrigAct is now true)")
        serviceScope.launch {
            try {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                delay(50)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                provideHapticFeedback(MediaAction.PLAY_PAUSE)
                logInfo("Dispatch MEDIA_PLAY_PAUSE OK")
            } catch (e: Exception) {
                logError("Err dispatch PLAY_PAUSE", e)
                showError("Fail play/pause")
            }
        }
    }    private fun provideHapticFeedback(action: MediaAction) {
        serviceScope.launch {
            try {
                val hapticEnabled = settingsManager.getHapticFeedbackEnabled.first()
                if (hapticEnabled) {
                    val vibrationMode = settingsManager.getVibrationMode.first()
                    logDebug("Providing haptic feedback: action=$action, mode=$vibrationMode")
                    
                    when (vibrationMode) {
                        "CUSTOM" -> {
                            val customSettings = settingsManager.getCustomVibrationSettings.first()
                            logDebug("Custom vibration settings: $customSettings")
                            vibrationManager.provideHapticFeedback(action, vibrationMode, customSettings)
                        }
                        "DISTINCT" -> {
                            val distinctSettings = settingsManager.getDistinctSettings.first()
                            logDebug("Distinct pattern settings: $distinctSettings")
                            vibrationManager.provideHapticFeedback(action, vibrationMode, distinctSettings = distinctSettings)
                        }                        "ADVANCED" -> {
                            val advancedPatterns = settingsManager.getAdvancedPatterns.first()
                            logDebug("Advanced pattern settings: $advancedPatterns")
                            vibrationManager.provideHapticFeedback(action, vibrationMode, advancedPatterns = advancedPatterns)
                        }
                        else -> {
                            vibrationManager.provideHapticFeedback(action, vibrationMode)
                        }
                    }
                } else {
                    logDebug("Haptic feedback disabled in settings")
                }
            } catch (e: Exception) {
                logError("Err haptic feedback", e)
            }
        }
    }
    private fun showError(message: String) {
        serviceScope.launch(Dispatchers.Main.immediate) {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                logError("Err show Toast: '$message'", e)
            }
        }
    }

    private fun resetAllStates() {
        logInfo("resetAllStates. Prev:VUp=$isVolumeUpPressed,VDown=$isVolumeDownPressed,TrigAct=$hasTriggeredAction. Setting TrigAct=false.")
        isVolumeUpPressed = false; isVolumeDownPressed = false
        volumeUpPressTime = 0L; volumeDownPressTime = 0L
        hasTriggeredAction = false
        cancelAllActionTimeouts()
        logDebug("resetAllStates completed.")
    }

    private fun resetAllStatesAndJobs() {
        logWarn("resetAllStatesAndJobs called (likely from lifecycle/error).")
        resetAllStates()
    }
}
