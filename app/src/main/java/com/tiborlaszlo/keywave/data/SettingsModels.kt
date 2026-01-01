package com.tiborlaszlo.keywave.data

enum class ActionType {
  NEXT,
  PREVIOUS,
  PLAY_PAUSE,
  STOP,
  MUTE,
  LAUNCH_APP,
  FLASHLIGHT,
  ASSISTANT,
  SPOTIFY_LIKE,
  NONE,
}

enum class HapticPattern {
  OFF,
  SHORT,
  LONG,
  DOUBLE,
  CUSTOM,
}

enum class HardwareKey {
  VOLUME_UP,
  VOLUME_DOWN,
}

enum class PressType {
  SHORT,
  LONG,
}

enum class ActivationMode {
  ALWAYS,
  MEDIA_ACTIVE,
  MEDIA_PLAYING,
}

enum class ScreenStateMode {
  ANY,
  SCREEN_ON_ONLY,
  SCREEN_OFF_ONLY,
}

data class ButtonConfig(
  val enabled: Boolean,
  val action: ActionType,
  val longPressMs: Long,
  val hapticPattern: HapticPattern,
  val hapticIntensity: Int,
  val customPulseCount: Int,
  val customPulseMs: Int,
  val customGapMs: Int,
)

data class ComboConfig(
  val enabled: Boolean,
  val action: ActionType,
  val longPressMs: Long,
  val hapticPattern: HapticPattern,
  val hapticIntensity: Int,
  val customPulseCount: Int,
  val customPulseMs: Int,
  val customGapMs: Int,
)

data class KeybindStep(
  val key: HardwareKey,
  val pressType: PressType,
  val longPressMs: Long,
  val delayAfterMs: Long,
)

data class CustomKeybind(
  val id: String,
  val name: String,
  val enabled: Boolean,
  val action: ActionType,
  val hapticPattern: HapticPattern,
  val hapticIntensity: Int,
  val customPulseCount: Int,
  val customPulseMs: Int,
  val customGapMs: Int,
  val steps: List<KeybindStep>,
)

data class SettingsState(
  val serviceEnabled: Boolean,
  val volumeUp: ButtonConfig,
  val volumeDown: ButtonConfig,
  val combo: ComboConfig,
  val customKeybinds: List<CustomKeybind>,
  val activationMode: ActivationMode,
  val screenStateMode: ScreenStateMode,
  val enableNext: Boolean,
  val enablePrevious: Boolean,
  val enablePlayPause: Boolean,
  val enableStop: Boolean,
  val enableMute: Boolean,
  val enableLaunchApp: Boolean,
  val enableFlashlight: Boolean,
  val enableAssistant: Boolean,
  val enableSpotifyLike: Boolean,
  val allowlist: Set<String>,
  val blocklist: Set<String>,
  val debugEnabled: Boolean = false,
)

object DefaultSettings {
  const val longPressMs: Long = 600
  private const val defaultIntensity = 200
  private const val defaultPulseCount = 2
  private const val defaultPulseMs = 40
  private const val defaultGapMs = 60

  val volumeUp = ButtonConfig(
    enabled = true,
    action = ActionType.NEXT,
    longPressMs = longPressMs,
    hapticPattern = HapticPattern.SHORT,
    hapticIntensity = defaultIntensity,
    customPulseCount = defaultPulseCount,
    customPulseMs = defaultPulseMs,
    customGapMs = defaultGapMs,
  )
  val volumeDown = ButtonConfig(
    enabled = true,
    action = ActionType.PREVIOUS,
    longPressMs = longPressMs,
    hapticPattern = HapticPattern.SHORT,
    hapticIntensity = defaultIntensity,
    customPulseCount = defaultPulseCount,
    customPulseMs = defaultPulseMs,
    customGapMs = defaultGapMs,
  )
  val combo = ComboConfig(
    enabled = true,
    action = ActionType.PLAY_PAUSE,
    longPressMs = longPressMs,
    hapticPattern = HapticPattern.SHORT,
    hapticIntensity = defaultIntensity,
    customPulseCount = defaultPulseCount,
    customPulseMs = defaultPulseMs,
    customGapMs = defaultGapMs,
  )

  val state = SettingsState(
    serviceEnabled = true,
    volumeUp = volumeUp,
    volumeDown = volumeDown,
    combo = combo,
    customKeybinds = emptyList(),
    activationMode = ActivationMode.MEDIA_ACTIVE,
    screenStateMode = ScreenStateMode.ANY,
    enableNext = true,
    enablePrevious = true,
    enablePlayPause = true,
    enableStop = true,
    enableMute = true,
    enableLaunchApp = true,
    enableFlashlight = true,
    enableAssistant = true,
    enableSpotifyLike = true,
    allowlist = emptySet(),
    blocklist = emptySet(),
    debugEnabled = false,
  )
}
