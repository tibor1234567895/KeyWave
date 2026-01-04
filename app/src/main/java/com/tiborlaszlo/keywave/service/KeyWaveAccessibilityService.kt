package com.tiborlaszlo.keywave.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.tiborlaszlo.keywave.core.ActionDispatcher
import com.tiborlaszlo.keywave.core.CustomKeybindMatcher
import com.tiborlaszlo.keywave.core.DeviceState
import com.tiborlaszlo.keywave.core.HapticFeedback
import com.tiborlaszlo.keywave.core.MediaSessionHelper
import com.tiborlaszlo.keywave.core.SettingsManager
import com.tiborlaszlo.keywave.core.VolumeKeyGestureDetector
import com.tiborlaszlo.keywave.data.ActionType
import com.tiborlaszlo.keywave.data.ScreenStateMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import android.accessibilityservice.AccessibilityServiceInfo

class KeyWaveAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "KeyWaveA11y"
        const val ACTION_SYSTEM_INTERCEPTION_DETECTED = "com.tiborlaszlo.keywave.SYSTEM_INTERCEPTION_DETECTED"
        const val ACTION_SERVICE_CONNECTED = "com.tiborlaszlo.keywave.SERVICE_CONNECTED"
    }

    private lateinit var serviceScope: CoroutineScope
    private lateinit var settingsManager: SettingsManager
    private lateinit var actionDispatcher: ActionDispatcher
    private lateinit var gestureDetector: VolumeKeyGestureDetector
    private lateinit var deviceState: DeviceState
    private lateinit var hapticFeedback: HapticFeedback
    private lateinit var customKeybindMatcher: CustomKeybindMatcher

    private val isDebugEnabled: Boolean
        get() = if (::settingsManager.isInitialized) settingsManager.state.value.debugEnabled else false

    private fun shouldConsumeVolumeKeyEvents(): Boolean {
        if (!::settingsManager.isInitialized) return false
        
        val state = settingsManager.state.value
        if (!state.serviceEnabled) return false
        
        val screenOn = deviceState.isScreenOn()
        val screenAllowed = when (state.screenStateMode) {
            ScreenStateMode.ANY -> true
            ScreenStateMode.SCREEN_ON_ONLY -> screenOn
            ScreenStateMode.SCREEN_OFF_ONLY -> !screenOn
        }
        
        return screenAllowed
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        // ALWAYS log service connection (not gated by debug)
        Log.w(TAG, "=== KeyWave Accessibility Service CONNECTED ===")
        
        // Bring app back to foreground
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            launchIntent?.let { startActivity(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not bring app to foreground: ${e.message}")
        }

        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        settingsManager = SettingsManager(this, serviceScope)
        deviceState = DeviceState(this)
        hapticFeedback = HapticFeedback(this) { isDebugEnabled }
        customKeybindMatcher = CustomKeybindMatcher()
        val mediaHelper = MediaSessionHelper(
            this,
            ComponentName(this, KeyWaveNotificationListener::class.java),
        )
        actionDispatcher = ActionDispatcher(mediaHelper)
        gestureDetector = VolumeKeyGestureDetector(
            context = this,
            volumeUpThresholdMs = { settingsManager.state.value.volumeUp.longPressMs },
            volumeDownThresholdMs = { settingsManager.state.value.volumeDown.longPressMs },
            comboThresholdMs = { settingsManager.state.value.combo.longPressMs },
            onLongPress = { keyCode -> handleLongPress(keyCode) },
            onBothLongPress = { handleBothLongPress() },
            onSystemInterceptionDetected = { handleSystemInterception() },
            isDebugEnabled = { isDebugEnabled },
            shouldConsumeEvent = { shouldConsumeVolumeKeyEvents() },
        )
        
        Log.w(TAG, "Service initialized. Debug=${isDebugEnabled}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used - we only need key events
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        
        // ALWAYS log key events (not gated by debug) - this is critical for debugging
        val keyName = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> "VOL_UP"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "VOL_DOWN"
            else -> "OTHER(${event.keyCode})"
        }
        val actionName = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        Log.w(TAG, "onKeyEvent: $keyName $actionName repeat=${event.repeatCount}")
        
        val state = settingsManager.state.value
        customKeybindMatcher.onKeyEvent(event, state.customKeybinds) { keybind ->
            handleCustomKeybind(keybind)
        }
        val consumed = gestureDetector.onKeyEvent(event)
        Log.w(TAG, "Event consumed: $consumed")
        return consumed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "=== KeyWave Accessibility Service DESTROYED ===")
        if (this::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
    }

    private fun handleLongPress(keyCode: Int) {
        Log.w(TAG, ">>> handleLongPress called for keyCode=$keyCode")
        
        val state = settingsManager.state.value
        if (!state.serviceEnabled) {
            Log.w(TAG, "Long press ignored: service disabled")
            return
        }
        val screenOn = deviceState.isScreenOn()
        val screenAllowed = when (state.screenStateMode) {
            ScreenStateMode.ANY -> true
            ScreenStateMode.SCREEN_ON_ONLY -> screenOn
            ScreenStateMode.SCREEN_OFF_ONLY -> !screenOn
        }
        if (!screenAllowed) {
            Log.w(TAG, "Long press ignored: screen state not allowed (screenOn=$screenOn, mode=${state.screenStateMode})")
            return
        }
        val (action, haptic) = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> state.volumeUp.action to state.volumeUp
            KeyEvent.KEYCODE_VOLUME_DOWN -> state.volumeDown.action to state.volumeDown
            else -> return
        }
        if (!haptic.enabled) {
            Log.w(TAG, "Long press ignored: button disabled")
            return
        }
        if (!isActionEnabled(action, state)) {
            Log.w(TAG, "Long press ignored: action $action not enabled")
            return
        }
        
        Log.w(TAG, ">>> Dispatching action: $action")
        actionDispatcher.dispatch(action, state)
        
        Log.w(TAG, ">>> Performing haptic: ${haptic.hapticPattern}, intensity=${haptic.hapticIntensity}")
        hapticFeedback.perform(
            haptic.hapticPattern,
            haptic.hapticIntensity,
            haptic.customPulseCount,
            haptic.customPulseMs,
            haptic.customGapMs,
        )
    }

    private fun handleBothLongPress() {
        Log.w(TAG, ">>> handleBothLongPress called")
        
        val state = settingsManager.state.value
        val combo = state.combo
        if (!combo.enabled) {
            Log.w(TAG, "Combo ignored: disabled")
            return
        }
        if (!state.serviceEnabled) {
            Log.w(TAG, "Combo ignored: service disabled")
            return
        }
        val screenOn = deviceState.isScreenOn()
        val screenAllowed = when (state.screenStateMode) {
            ScreenStateMode.ANY -> true
            ScreenStateMode.SCREEN_ON_ONLY -> screenOn
            ScreenStateMode.SCREEN_OFF_ONLY -> !screenOn
        }
        if (!screenAllowed) {
            Log.w(TAG, "Combo ignored: screen state not allowed")
            return
        }
        if (!isActionEnabled(combo.action, state)) {
            Log.w(TAG, "Combo ignored: action ${combo.action} not enabled")
            return
        }
        
        Log.w(TAG, ">>> Dispatching combo action: ${combo.action}")
        actionDispatcher.dispatch(combo.action, state)
        
        Log.w(TAG, ">>> Performing combo haptic: ${combo.hapticPattern}")
        hapticFeedback.perform(
            combo.hapticPattern,
            combo.hapticIntensity,
            combo.customPulseCount,
            combo.customPulseMs,
            combo.customGapMs,
        )
    }

    private fun handleCustomKeybind(keybind: com.tiborlaszlo.keywave.data.CustomKeybind) {
        val state = settingsManager.state.value
        if (!state.serviceEnabled) return
        val screenOn = deviceState.isScreenOn()
        val screenAllowed = when (state.screenStateMode) {
            ScreenStateMode.ANY -> true
            ScreenStateMode.SCREEN_ON_ONLY -> screenOn
            ScreenStateMode.SCREEN_OFF_ONLY -> !screenOn
        }
        if (!screenAllowed) return
        if (!keybind.enabled) return
        if (!isActionEnabled(keybind.action, state)) return
        
        Log.w(TAG, "Custom keybind triggered: ${keybind.name} -> ${keybind.action}")
        actionDispatcher.dispatch(keybind.action, state)
        hapticFeedback.perform(
            keybind.hapticPattern,
            keybind.hapticIntensity,
            keybind.customPulseCount,
            keybind.customPulseMs,
            keybind.customGapMs,
        )
    }

    private fun isActionEnabled(action: ActionType, state: com.tiborlaszlo.keywave.data.SettingsState): Boolean {
        return when (action) {
            ActionType.NEXT -> state.enableNext
            ActionType.PREVIOUS -> state.enablePrevious
            ActionType.PLAY_PAUSE -> state.enablePlayPause
            ActionType.STOP -> state.enableStop
            ActionType.MUTE -> state.enableMute
            ActionType.LAUNCH_APP -> state.enableLaunchApp
            ActionType.FLASHLIGHT -> state.enableFlashlight
            ActionType.ASSISTANT -> state.enableAssistant
            ActionType.SPOTIFY_LIKE -> state.enableSpotifyLike
            ActionType.NONE -> false
        }
    }
    
    private fun handleSystemInterception() {
        Log.w(TAG, "!!! System interception detected - notifying UI")
        // Broadcast to notify the app that system shortcuts may be stealing key events
        val intent = android.content.Intent(ACTION_SYSTEM_INTERCEPTION_DETECTED)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }
}
