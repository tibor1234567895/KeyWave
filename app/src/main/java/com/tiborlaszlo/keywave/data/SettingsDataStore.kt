package com.tiborlaszlo.keywave.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "settings"

val Context.settingsDataStore by preferencesDataStore(name = DATASTORE_NAME)

class SettingsRepository(private val context: Context) {
  private object Keys {
    val serviceEnabled = booleanPreferencesKey("service_enabled")
    val volumeUpEnabled = booleanPreferencesKey("volume_up_enabled")
    val volumeDownEnabled = booleanPreferencesKey("volume_down_enabled")
    val volumeUpAction = stringPreferencesKey("volume_up_action")
    val volumeDownAction = stringPreferencesKey("volume_down_action")
    val volumeUpLongPressMs = longPreferencesKey("volume_up_long_press_ms")
    val volumeDownLongPressMs = longPreferencesKey("volume_down_long_press_ms")
    val volumeUpHapticPattern = stringPreferencesKey("volume_up_haptic_pattern")
    val volumeDownHapticPattern = stringPreferencesKey("volume_down_haptic_pattern")
    val volumeUpHapticIntensity = intPreferencesKey("volume_up_haptic_intensity")
    val volumeDownHapticIntensity = intPreferencesKey("volume_down_haptic_intensity")
    val volumeUpCustomPulseCount = intPreferencesKey("volume_up_custom_pulse_count")
    val volumeDownCustomPulseCount = intPreferencesKey("volume_down_custom_pulse_count")
    val volumeUpCustomPulseMs = intPreferencesKey("volume_up_custom_pulse_ms")
    val volumeDownCustomPulseMs = intPreferencesKey("volume_down_custom_pulse_ms")
    val volumeUpCustomGapMs = intPreferencesKey("volume_up_custom_gap_ms")
    val volumeDownCustomGapMs = intPreferencesKey("volume_down_custom_gap_ms")
    val comboEnabled = booleanPreferencesKey("combo_enabled")
    val comboAction = stringPreferencesKey("combo_action")
    val comboLongPressMs = longPreferencesKey("combo_long_press_ms")
    val comboHapticPattern = stringPreferencesKey("combo_haptic_pattern")
    val comboHapticIntensity = intPreferencesKey("combo_haptic_intensity")
    val comboCustomPulseCount = intPreferencesKey("combo_custom_pulse_count")
    val comboCustomPulseMs = intPreferencesKey("combo_custom_pulse_ms")
    val comboCustomGapMs = intPreferencesKey("combo_custom_gap_ms")
    val activationMode = stringPreferencesKey("activation_mode")
    val screenStateMode = stringPreferencesKey("screen_state_mode")
    val enableNext = booleanPreferencesKey("enable_next")
    val enablePrevious = booleanPreferencesKey("enable_previous")
    val enablePlayPause = booleanPreferencesKey("enable_play_pause")
    val enableStop = booleanPreferencesKey("enable_stop")
    val enableMute = booleanPreferencesKey("enable_mute")
    val enableLaunchApp = booleanPreferencesKey("enable_launch_app")
    val enableFlashlight = booleanPreferencesKey("enable_flashlight")
    val enableAssistant = booleanPreferencesKey("enable_assistant")
    val enableSpotifyLike = booleanPreferencesKey("enable_spotify_like")
    val allowlist = stringSetPreferencesKey("allowlist")
    val blocklist = stringSetPreferencesKey("blocklist")
    val customKeybinds = stringPreferencesKey("custom_keybinds")
    val debugEnabled = booleanPreferencesKey("debug_enabled")
  }

  val settings: Flow<SettingsState> = context.settingsDataStore.data.map { prefs ->
    val volumeUpLongPressMs =
      prefs[Keys.volumeUpLongPressMs] ?: DefaultSettings.volumeUp.longPressMs
    val volumeDownLongPressMs =
      prefs[Keys.volumeDownLongPressMs] ?: DefaultSettings.volumeDown.longPressMs
    val volumeUpAction = ActionType.valueOf(
      prefs[Keys.volumeUpAction] ?: DefaultSettings.volumeUp.action.name
    )
    val volumeDownAction = ActionType.valueOf(
      prefs[Keys.volumeDownAction] ?: DefaultSettings.volumeDown.action.name
    )
    val volumeUpHapticPattern = HapticPattern.valueOf(
      prefs[Keys.volumeUpHapticPattern] ?: DefaultSettings.volumeUp.hapticPattern.name
    )
    val volumeDownHapticPattern = HapticPattern.valueOf(
      prefs[Keys.volumeDownHapticPattern] ?: DefaultSettings.volumeDown.hapticPattern.name
    )
    val activationMode = ActivationMode.valueOf(
      prefs[Keys.activationMode] ?: DefaultSettings.state.activationMode.name
    )
    val screenStateMode = ScreenStateMode.valueOf(
      prefs[Keys.screenStateMode] ?: DefaultSettings.state.screenStateMode.name
    )
    val comboEnabled = prefs[Keys.comboEnabled] ?: DefaultSettings.combo.enabled
    val comboAction = ActionType.valueOf(
      prefs[Keys.comboAction] ?: DefaultSettings.combo.action.name
    )
    val comboLongPressMs = prefs[Keys.comboLongPressMs] ?: DefaultSettings.combo.longPressMs
    val comboHapticPattern = HapticPattern.valueOf(
      prefs[Keys.comboHapticPattern] ?: DefaultSettings.combo.hapticPattern.name
    )

    val customKeybinds = parseCustomKeybinds(prefs[Keys.customKeybinds])

    SettingsState(
      serviceEnabled = prefs[Keys.serviceEnabled] ?: DefaultSettings.state.serviceEnabled,
      volumeUp = ButtonConfig(
        enabled = prefs[Keys.volumeUpEnabled] ?: DefaultSettings.volumeUp.enabled,
        action = volumeUpAction,
        longPressMs = volumeUpLongPressMs,
        hapticPattern = volumeUpHapticPattern,
        hapticIntensity = prefs[Keys.volumeUpHapticIntensity]
          ?: DefaultSettings.volumeUp.hapticIntensity,
        customPulseCount = prefs[Keys.volumeUpCustomPulseCount]
          ?: DefaultSettings.volumeUp.customPulseCount,
        customPulseMs = prefs[Keys.volumeUpCustomPulseMs]
          ?: DefaultSettings.volumeUp.customPulseMs,
        customGapMs = prefs[Keys.volumeUpCustomGapMs]
          ?: DefaultSettings.volumeUp.customGapMs,
      ),
      volumeDown = ButtonConfig(
        enabled = prefs[Keys.volumeDownEnabled] ?: DefaultSettings.volumeDown.enabled,
        action = volumeDownAction,
        longPressMs = volumeDownLongPressMs,
        hapticPattern = volumeDownHapticPattern,
        hapticIntensity = prefs[Keys.volumeDownHapticIntensity]
          ?: DefaultSettings.volumeDown.hapticIntensity,
        customPulseCount = prefs[Keys.volumeDownCustomPulseCount]
          ?: DefaultSettings.volumeDown.customPulseCount,
        customPulseMs = prefs[Keys.volumeDownCustomPulseMs]
          ?: DefaultSettings.volumeDown.customPulseMs,
        customGapMs = prefs[Keys.volumeDownCustomGapMs]
          ?: DefaultSettings.volumeDown.customGapMs,
      ),
      combo = ComboConfig(
        enabled = comboEnabled,
        action = comboAction,
        longPressMs = comboLongPressMs,
        hapticPattern = comboHapticPattern,
        hapticIntensity = prefs[Keys.comboHapticIntensity]
          ?: DefaultSettings.combo.hapticIntensity,
        customPulseCount = prefs[Keys.comboCustomPulseCount]
          ?: DefaultSettings.combo.customPulseCount,
        customPulseMs = prefs[Keys.comboCustomPulseMs]
          ?: DefaultSettings.combo.customPulseMs,
        customGapMs = prefs[Keys.comboCustomGapMs]
          ?: DefaultSettings.combo.customGapMs,
      ),
      activationMode = activationMode,
      screenStateMode = screenStateMode,
      customKeybinds = customKeybinds,
      enableNext = prefs[Keys.enableNext] ?: DefaultSettings.state.enableNext,
      enablePrevious = prefs[Keys.enablePrevious] ?: DefaultSettings.state.enablePrevious,
      enablePlayPause = prefs[Keys.enablePlayPause] ?: DefaultSettings.state.enablePlayPause,
      enableStop = prefs[Keys.enableStop] ?: DefaultSettings.state.enableStop,
      enableMute = prefs[Keys.enableMute] ?: DefaultSettings.state.enableMute,
      enableLaunchApp = prefs[Keys.enableLaunchApp] ?: DefaultSettings.state.enableLaunchApp,
      enableFlashlight = prefs[Keys.enableFlashlight] ?: DefaultSettings.state.enableFlashlight,
      enableAssistant = prefs[Keys.enableAssistant] ?: DefaultSettings.state.enableAssistant,
      enableSpotifyLike = prefs[Keys.enableSpotifyLike] ?: DefaultSettings.state.enableSpotifyLike,
      allowlist = prefs[Keys.allowlist] ?: DefaultSettings.state.allowlist,
      blocklist = prefs[Keys.blocklist] ?: DefaultSettings.state.blocklist,
      debugEnabled = prefs[Keys.debugEnabled] ?: DefaultSettings.state.debugEnabled,
    )
  }

  suspend fun setServiceEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.serviceEnabled] = enabled }
  }

  suspend fun setVolumeUpLongPressMs(value: Long) {
    context.settingsDataStore.edit { it[Keys.volumeUpLongPressMs] = value }
  }

  suspend fun setVolumeDownLongPressMs(value: Long) {
    context.settingsDataStore.edit { it[Keys.volumeDownLongPressMs] = value }
  }

  suspend fun setVolumeUpAction(action: ActionType) {
    context.settingsDataStore.edit { it[Keys.volumeUpAction] = action.name }
  }

  suspend fun setVolumeDownAction(action: ActionType) {
    context.settingsDataStore.edit { it[Keys.volumeDownAction] = action.name }
  }

  suspend fun setVolumeUpEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.volumeUpEnabled] = enabled }
  }

  suspend fun setVolumeDownEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.volumeDownEnabled] = enabled }
  }

  suspend fun setVolumeUpHapticPattern(pattern: HapticPattern) {
    context.settingsDataStore.edit { it[Keys.volumeUpHapticPattern] = pattern.name }
  }

  suspend fun setVolumeDownHapticPattern(pattern: HapticPattern) {
    context.settingsDataStore.edit { it[Keys.volumeDownHapticPattern] = pattern.name }
  }

  suspend fun setVolumeUpHapticIntensity(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeUpHapticIntensity] = value }
  }

  suspend fun setVolumeDownHapticIntensity(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeDownHapticIntensity] = value }
  }

  suspend fun setVolumeUpCustomPulseCount(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeUpCustomPulseCount] = value }
  }

  suspend fun setVolumeDownCustomPulseCount(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeDownCustomPulseCount] = value }
  }

  suspend fun setVolumeUpCustomPulseMs(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeUpCustomPulseMs] = value }
  }

  suspend fun setVolumeDownCustomPulseMs(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeDownCustomPulseMs] = value }
  }

  suspend fun setVolumeUpCustomGapMs(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeUpCustomGapMs] = value }
  }

  suspend fun setVolumeDownCustomGapMs(value: Int) {
    context.settingsDataStore.edit { it[Keys.volumeDownCustomGapMs] = value }
  }

  suspend fun setComboEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.comboEnabled] = enabled }
  }

  suspend fun setComboAction(action: ActionType) {
    context.settingsDataStore.edit { it[Keys.comboAction] = action.name }
  }

  suspend fun setComboLongPressMs(value: Long) {
    context.settingsDataStore.edit { it[Keys.comboLongPressMs] = value }
  }

  suspend fun setComboHapticPattern(pattern: HapticPattern) {
    context.settingsDataStore.edit { it[Keys.comboHapticPattern] = pattern.name }
  }

  suspend fun setComboHapticIntensity(value: Int) {
    context.settingsDataStore.edit { it[Keys.comboHapticIntensity] = value }
  }

  suspend fun setComboCustomPulseCount(value: Int) {
    context.settingsDataStore.edit { it[Keys.comboCustomPulseCount] = value }
  }

  suspend fun setComboCustomPulseMs(value: Int) {
    context.settingsDataStore.edit { it[Keys.comboCustomPulseMs] = value }
  }

  suspend fun setComboCustomGapMs(value: Int) {
    context.settingsDataStore.edit { it[Keys.comboCustomGapMs] = value }
  }

  suspend fun setActivationMode(mode: ActivationMode) {
    context.settingsDataStore.edit { it[Keys.activationMode] = mode.name }
  }

  suspend fun setScreenStateMode(mode: ScreenStateMode) {
    context.settingsDataStore.edit { it[Keys.screenStateMode] = mode.name }
  }

  suspend fun setFeatureEnabled(key: String, enabled: Boolean) {
    val prefKey = booleanPreferencesKey(key)
    context.settingsDataStore.edit { it[prefKey] = enabled }
  }

  suspend fun setEnableNext(enabled: Boolean) = setFeatureEnabled("enable_next", enabled)
  suspend fun setEnablePrevious(enabled: Boolean) =
    setFeatureEnabled("enable_previous", enabled)
  suspend fun setEnablePlayPause(enabled: Boolean) =
    setFeatureEnabled("enable_play_pause", enabled)
  suspend fun setEnableStop(enabled: Boolean) = setFeatureEnabled("enable_stop", enabled)
  suspend fun setEnableMute(enabled: Boolean) = setFeatureEnabled("enable_mute", enabled)
  suspend fun setEnableLaunchApp(enabled: Boolean) =
    setFeatureEnabled("enable_launch_app", enabled)
  suspend fun setEnableFlashlight(enabled: Boolean) =
    setFeatureEnabled("enable_flashlight", enabled)
  suspend fun setEnableAssistant(enabled: Boolean) =
    setFeatureEnabled("enable_assistant", enabled)
  suspend fun setEnableSpotifyLike(enabled: Boolean) =
    setFeatureEnabled("enable_spotify_like", enabled)

  suspend fun setAllowlist(packages: Set<String>) {
    context.settingsDataStore.edit { it[Keys.allowlist] = packages }
  }

  suspend fun setBlocklist(packages: Set<String>) {
    context.settingsDataStore.edit { it[Keys.blocklist] = packages }
  }

  suspend fun setCustomKeybinds(keybinds: List<CustomKeybind>) {
    val json = serializeCustomKeybinds(keybinds)
    context.settingsDataStore.edit { it[Keys.customKeybinds] = json }
  }

  suspend fun setDebugEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.debugEnabled] = enabled }
  }

  private fun parseCustomKeybinds(raw: String?): List<CustomKeybind> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
      val array = JSONArray(raw)
      val results = ArrayList<CustomKeybind>(array.length())
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val stepsJson = obj.optJSONArray("steps") ?: JSONArray()
        val steps = ArrayList<KeybindStep>(stepsJson.length())
        for (j in 0 until stepsJson.length()) {
          val stepObj = stepsJson.getJSONObject(j)
          steps.add(
            KeybindStep(
              key = HardwareKey.valueOf(stepObj.getString("key")),
              pressType = PressType.valueOf(stepObj.getString("pressType")),
              longPressMs = stepObj.optLong("longPressMs", 600),
              delayAfterMs = stepObj.optLong("delayAfterMs", 600),
            )
          )
        }
        results.add(
          CustomKeybind(
            id = obj.getString("id"),
            name = obj.getString("name"),
            enabled = obj.optBoolean("enabled", true),
            action = ActionType.valueOf(obj.getString("action")),
            hapticPattern = HapticPattern.valueOf(obj.getString("hapticPattern")),
            hapticIntensity = obj.optInt("hapticIntensity", 200),
            customPulseCount = obj.optInt("customPulseCount", 2),
            customPulseMs = obj.optInt("customPulseMs", 40),
            customGapMs = obj.optInt("customGapMs", 60),
            steps = steps,
          )
        )
      }
      results
    } catch (e: Exception) {
      android.util.Log.e("SettingsRepository", "Failed to parse custom keybinds", e)
      emptyList()
    }
  }

  private fun serializeCustomKeybinds(keybinds: List<CustomKeybind>): String {
    val array = JSONArray()
    keybinds.forEach { kb ->
      val obj = JSONObject()
      obj.put("id", kb.id)
      obj.put("name", kb.name)
      obj.put("enabled", kb.enabled)
      obj.put("action", kb.action.name)
      obj.put("hapticPattern", kb.hapticPattern.name)
      obj.put("hapticIntensity", kb.hapticIntensity)
      obj.put("customPulseCount", kb.customPulseCount)
      obj.put("customPulseMs", kb.customPulseMs)
      obj.put("customGapMs", kb.customGapMs)
      val steps = JSONArray()
      kb.steps.forEach { step ->
        val stepObj = JSONObject()
        stepObj.put("key", step.key.name)
        stepObj.put("pressType", step.pressType.name)
        stepObj.put("longPressMs", step.longPressMs)
        stepObj.put("delayAfterMs", step.delayAfterMs)
        steps.put(stepObj)
      }
      obj.put("steps", steps)
      array.put(obj)
    }
    return array.toString()
  }
}
