package eu.tiborlaszlo.keywave.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.serialization.Serializable

// Data classes for advanced vibration patterns
@Serializable
data class VibrationPattern(
    val pulses: List<VibrationPulse> = listOf(VibrationPulse()),
    val repeatCount: Int = 0 // 0 = no repeat, -1 = infinite repeat
)

@Serializable
data class VibrationPulse(
    val duration: Long = 50,     // Duration in milliseconds
    val amplitude: Int = 255,    // Amplitude 0-255 (0 = off, 255 = max)
    val pauseAfter: Long = 0     // Pause after this pulse in milliseconds
)

// Data classes for configurable distinct patterns
data class DistinctPatternSettings(
    val nextTrackPulses: Int = 2,
    val nextTrackIntensity: Int = 255,
    val previousTrackPulses: Int = 1,
    val previousTrackIntensity: Int = 255,
    val playPausePulses: Int = 3,
    val playPauseIntensity: Int = 255
)

enum class VibrationMode {
    OFF,           // No vibration
    SIMPLE,        // Single short vibration for all actions
    DISTINCT,      // Different patterns for each action
    GENTLE,        // Very gentle vibrations (renamed from SUBTLE)
    STRONG,        // More intense vibrations
    CUSTOM,        // Custom per-action vibration modes
    ADVANCED       // Advanced custom patterns with full control
}

enum class MediaAction {
    NEXT_TRACK,
    PREVIOUS_TRACK,
    PLAY_PAUSE
}

class VibrationManager(context: Context) {
    private val vibrator: Vibrator = context.getSystemService(Vibrator::class.java)
    private val canVibrate: Boolean = vibrator.hasVibrator()
    private val tag = "VibrationManager"
    
    fun provideHapticFeedback(
        action: MediaAction, 
        vibrationMode: String, 
        customSettings: Map<String, String>? = null,
        advancedPatterns: Map<String, VibrationPattern>? = null,
        distinctSettings: DistinctPatternSettings? = null
    ) {
        if (!canVibrate) {
            Log.d(tag, "Vibrator not available")
            return
        }
        
        val mode = try {
            VibrationMode.valueOf(vibrationMode)
        } catch (e: IllegalArgumentException) {
            Log.w(tag, "Unknown vibration mode: $vibrationMode, using SIMPLE")
            VibrationMode.SIMPLE
        }
          if (mode == VibrationMode.OFF) {
            Log.d(tag, "Vibration disabled")
            return
        }
        
        // For custom mode, check if the specific action is set to OFF
        if (mode == VibrationMode.CUSTOM && customSettings != null) {
            val actionKey = when (action) {
                MediaAction.NEXT_TRACK -> "next_track_vibration_mode"
                MediaAction.PREVIOUS_TRACK -> "previous_track_vibration_mode"
                MediaAction.PLAY_PAUSE -> "play_pause_vibration_mode"
            }
            val customModeString = customSettings[actionKey] ?: "SIMPLE"
            if (customModeString == "OFF") {
                Log.d(tag, "Custom vibration OFF for $action")
                return
            }
        }
          val vibrationEffect = when {
            mode == VibrationMode.CUSTOM && customSettings != null -> {
                createCustomVibrationEffect(action, customSettings)
            }
            mode == VibrationMode.ADVANCED && advancedPatterns != null -> {
                createAdvancedVibrationEffect(action, advancedPatterns)
            }
            mode == VibrationMode.DISTINCT && distinctSettings != null -> {
                createConfigurableDistinctEffect(action, distinctSettings)
            }
            else -> {
                createVibrationEffect(action, mode, distinctSettings)
            }
        }
        
        try {
            vibrator.vibrate(vibrationEffect)
            Log.d(tag, "Vibration executed for $action with mode $mode")
        } catch (e: Exception) {
            Log.e(tag, "Error executing vibration", e)
        }
    }
      private fun createCustomVibrationEffect(action: MediaAction, customSettings: Map<String, String>): VibrationEffect {
        val actionKey = when (action) {
            MediaAction.NEXT_TRACK -> "next_track_vibration_mode"
            MediaAction.PREVIOUS_TRACK -> "previous_track_vibration_mode"
            MediaAction.PLAY_PAUSE -> "play_pause_vibration_mode"
        }
        
        val customModeString = customSettings[actionKey] ?: "SIMPLE"
        Log.d(tag, "Custom mode for $action: $customModeString (from key: $actionKey)")
        Log.d(tag, "Available custom settings: $customSettings")
          val customMode = try {
            VibrationMode.valueOf(customModeString)
        } catch (e: IllegalArgumentException) {
            Log.w(tag, "Invalid custom mode: $customModeString, using SIMPLE")
            VibrationMode.SIMPLE
        }
        
        // Handle OFF mode for custom settings - return null to indicate no vibration
        if (customMode == VibrationMode.OFF) {
            Log.d(tag, "Custom mode OFF for $action - no vibration")
            return VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE) // Minimal effect that won't be felt
        }
        
        return createVibrationEffect(action, customMode)
    }
    
    private fun createVibrationEffect(action: MediaAction, mode: VibrationMode, distinctSettings: DistinctPatternSettings? = null): VibrationEffect {
        return when (mode) {
            VibrationMode.OFF -> {
                // This shouldn't be called, but provide a minimal effect just in case
                VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            
            VibrationMode.SIMPLE -> {
                // Single short vibration for all actions
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            
            VibrationMode.DISTINCT -> {
                // Different patterns for each action
                when (action) {
                    MediaAction.NEXT_TRACK -> {
                        // Two quick pulses for next
                        val pattern = longArrayOf(0, 40, 30, 40)
                        VibrationEffect.createWaveform(pattern, -1)
                    }
                    MediaAction.PREVIOUS_TRACK -> {
                        // One longer pulse for previous
                        VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    MediaAction.PLAY_PAUSE -> {
                        // Three short pulses for play/pause
                        val pattern = longArrayOf(0, 30, 20, 30, 20, 30)
                        VibrationEffect.createWaveform(pattern, -1)
                    }
                }
            }
            VibrationMode.GENTLE -> {
                // Gentle single vibration for all actions (same pattern, low intensity)
                VibrationEffect.createOneShot(40, 80) // Low amplitude, consistent for all actions
            }            
            VibrationMode.STRONG -> {
                // Strong single vibration for all actions (same pattern, high intensity)  
                VibrationEffect.createOneShot(60, 255) // Max amplitude, consistent for all actions
            }
              VibrationMode.CUSTOM -> {
                // This should not be reached as CUSTOM mode uses createCustomVibrationEffect
                // Fall back to SIMPLE as a safety measure
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            
            VibrationMode.ADVANCED -> {
                // This should not be reached as ADVANCED mode uses createAdvancedVibrationEffect
                // Fall back to SIMPLE as a safety measure
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
        }
    }
      private fun createConfigurableDistinctEffect(action: MediaAction, settings: DistinctPatternSettings): VibrationEffect {
        return when (action) {
            MediaAction.NEXT_TRACK -> {
                createPulsePattern(settings.nextTrackPulses, settings.nextTrackIntensity)
            }
            MediaAction.PREVIOUS_TRACK -> {
                createPulsePattern(settings.previousTrackPulses, settings.previousTrackIntensity)
            }
            MediaAction.PLAY_PAUSE -> {
                createPulsePattern(settings.playPausePulses, settings.playPauseIntensity)
            }
        }
    }
    
    private fun createAdvancedVibrationEffect(action: MediaAction, patterns: Map<String, VibrationPattern>): VibrationEffect {
        val actionKey = when (action) {
            MediaAction.NEXT_TRACK -> "next_track_pattern"
            MediaAction.PREVIOUS_TRACK -> "previous_track_pattern"
            MediaAction.PLAY_PAUSE -> "play_pause_pattern"
        }
        
        val pattern = patterns[actionKey] ?: VibrationPattern() // Default to simple pattern
        return createFromVibrationPattern(pattern)
    }
    
    private fun createPulsePattern(pulseCount: Int, intensity: Int): VibrationEffect {
        if (pulseCount <= 0) {
            return VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        
        if (pulseCount == 1) {
            return VibrationEffect.createOneShot(50, intensity.coerceIn(1, 255))
        }
        
        // Create pattern with multiple pulses
        val pattern = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        
        pattern.add(0) // Initial delay
        amplitudes.add(0)
        
        repeat(pulseCount) { index ->
            pattern.add(40) // Pulse duration
            amplitudes.add(intensity.coerceIn(1, 255))
            
            if (index < pulseCount - 1) { // Add pause between pulses (except after last pulse)
                pattern.add(30) // Pause duration
                amplitudes.add(0)
            }
        }
        
        return VibrationEffect.createWaveform(pattern.toLongArray(), amplitudes.toIntArray(), -1)
    }
    
    private fun createFromVibrationPattern(pattern: VibrationPattern): VibrationEffect {
        if (pattern.pulses.isEmpty()) {
            return VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        
        if (pattern.pulses.size == 1 && pattern.pulses[0].pauseAfter == 0L) {
            // Single pulse, use simple method
            val pulse = pattern.pulses[0]
            return VibrationEffect.createOneShot(pulse.duration, pulse.amplitude.coerceIn(1, 255))
        }
        
        // Multiple pulses or complex pattern
        val timing = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        
        timing.add(0) // Initial delay
        amplitudes.add(0)
        
        pattern.pulses.forEach { pulse ->
            timing.add(pulse.duration)
            amplitudes.add(pulse.amplitude.coerceIn(0, 255))
            
            if (pulse.pauseAfter > 0) {
                timing.add(pulse.pauseAfter)
                amplitudes.add(0)
            }
        }
        
        return VibrationEffect.createWaveform(
            timing.toLongArray(), 
            amplitudes.toIntArray(), 
            pattern.repeatCount
        )
    }    
    companion object {
        fun getVibrationModeDisplayName(mode: String): String {
            return when (mode) {
                "OFF" -> "Off"
                "SIMPLE" -> "Simple"
                "DISTINCT" -> "Distinct per Action"
                "GENTLE" -> "Gentle"
                "STRONG" -> "Strong"
                "CUSTOM" -> "Custom"
                "ADVANCED" -> "Advanced Custom"
                else -> "Simple"
            }
        }
        
        fun getAllVibrationModes(): List<String> {
            return VibrationMode.values().map { it.name }
        }
        
        fun getActionDisplayName(action: MediaAction): String {
            return when (action) {
                MediaAction.NEXT_TRACK -> "Next Track"
                MediaAction.PREVIOUS_TRACK -> "Previous Track"
                MediaAction.PLAY_PAUSE -> "Play/Pause"
            }
        }
        
        fun getAvailableCustomModes(): List<String> {
            return listOf("OFF", "SIMPLE", "DISTINCT", "GENTLE", "STRONG")
        }
        
        fun getDefaultDistinctSettings(): DistinctPatternSettings {
            return DistinctPatternSettings()
        }
        
        fun getDefaultAdvancedPattern(): VibrationPattern {
            return VibrationPattern(
                pulses = listOf(VibrationPulse(duration = 50, amplitude = 255)),
                repeatCount = 0
            )
        }
        
        fun createDistinctSettingsFromConfig(
            nextPulses: Int, nextIntensity: Int,
            prevPulses: Int, prevIntensity: Int, 
            playPulses: Int, playIntensity: Int
        ): DistinctPatternSettings {
            return DistinctPatternSettings(
                nextTrackPulses = nextPulses.coerceIn(1, 10),
                nextTrackIntensity = nextIntensity.coerceIn(1, 255),
                previousTrackPulses = prevPulses.coerceIn(1, 10),
                previousTrackIntensity = prevIntensity.coerceIn(1, 255),
                playPausePulses = playPulses.coerceIn(1, 10),
                playPauseIntensity = playIntensity.coerceIn(1, 255)
            )
        }
    }}