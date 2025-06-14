package eu.tiborlaszlo.keywave.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import eu.tiborlaszlo.keywave.utils.DistinctPatternSettings
import eu.tiborlaszlo.keywave.utils.VibrationPattern
import eu.tiborlaszlo.keywave.utils.VibrationPulse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        private val NEXT_TRACK_THRESHOLD = intPreferencesKey("next_track_threshold")
        private val PREVIOUS_TRACK_THRESHOLD = intPreferencesKey("previous_track_threshold")
        private val PLAY_PAUSE_THRESHOLD = intPreferencesKey("play_pause_threshold")
        private val SIMULTANEOUS_PRESS_BUFFER = intPreferencesKey("simultaneous_press_buffer")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        private val VIBRATION_MODE = stringPreferencesKey("vibration_mode")
        private val APP_ENABLED = booleanPreferencesKey("app_enabled")
          // Custom vibration mode settings per action
        private val NEXT_TRACK_VIBRATION_MODE = stringPreferencesKey("next_track_vibration_mode")
        private val PREVIOUS_TRACK_VIBRATION_MODE = stringPreferencesKey("previous_track_vibration_mode")
        private val PLAY_PAUSE_VIBRATION_MODE = stringPreferencesKey("play_pause_vibration_mode")
        
        // Distinct pattern configuration
        private val DISTINCT_NEXT_PULSES = intPreferencesKey("distinct_next_pulses")
        private val DISTINCT_NEXT_INTENSITY = intPreferencesKey("distinct_next_intensity")
        private val DISTINCT_PREV_PULSES = intPreferencesKey("distinct_prev_pulses")
        private val DISTINCT_PREV_INTENSITY = intPreferencesKey("distinct_prev_intensity")
        private val DISTINCT_PLAY_PULSES = intPreferencesKey("distinct_play_pulses")
        private val DISTINCT_PLAY_INTENSITY = intPreferencesKey("distinct_play_intensity")
        
        // Advanced pattern settings (stored as JSON strings)
        private val ADVANCED_NEXT_PATTERN = stringPreferencesKey("advanced_next_pattern")
        private val ADVANCED_PREV_PATTERN = stringPreferencesKey("advanced_prev_pattern")
        private val ADVANCED_PLAY_PATTERN = stringPreferencesKey("advanced_play_pattern")        // New key for debug monitoring
        private val DEBUG_MONITORING_ENABLED = booleanPreferencesKey("debug_monitoring_enabled")
        
        // Constants
        const val DEFAULT_NEXT_TRACK_THRESHOLD = 800L
        const val DEFAULT_PREVIOUS_TRACK_THRESHOLD = 800L
        const val DEFAULT_PLAY_PAUSE_THRESHOLD = 1000L
        const val DEFAULT_SIMULTANEOUS_BUFFER = 300L // 300ms buffer for simultaneous press
        const val DEFAULT_DEBUG_MONITORING_ENABLED = false // Default for debug monitoring
        const val DEFAULT_VIBRATION_MODE = "SIMPLE" // Default vibration mode
        
        // Default distinct pattern settings
        const val DEFAULT_DISTINCT_NEXT_PULSES = 2
        const val DEFAULT_DISTINCT_NEXT_INTENSITY = 255
        const val DEFAULT_DISTINCT_PREV_PULSES = 1
        const val DEFAULT_DISTINCT_PREV_INTENSITY = 255
        const val DEFAULT_DISTINCT_PLAY_PULSES = 3
        const val DEFAULT_DISTINCT_PLAY_INTENSITY = 255

        const val MIN_THRESHOLD = 200L
        const val MAX_THRESHOLD = 2000L
        //const val MIN_BUFFER = 100L
        //const val MAX_BUFFER = 1000L
    }

    val getNextTrackThreshold: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[NEXT_TRACK_THRESHOLD]?.toLong() ?: DEFAULT_NEXT_TRACK_THRESHOLD
        }

    val getPreviousTrackThreshold: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PREVIOUS_TRACK_THRESHOLD]?.toLong() ?: DEFAULT_PREVIOUS_TRACK_THRESHOLD
        }

    val getPlayPauseThreshold: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PLAY_PAUSE_THRESHOLD]?.toLong() ?: DEFAULT_PLAY_PAUSE_THRESHOLD
        }

    val getSimultaneousPressBuffer: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[SIMULTANEOUS_PRESS_BUFFER]?.toLong() ?: DEFAULT_SIMULTANEOUS_BUFFER
        }

    val getHapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] != false
        }

    val isAppEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APP_ENABLED] != false
        }    // Flow for debug monitoring enabled state
    val getDebugMonitoringEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DEBUG_MONITORING_ENABLED] ?: DEFAULT_DEBUG_MONITORING_ENABLED
        }
    
    // Flow for vibration mode
    val getVibrationMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE
        }
    
    // Flows for custom vibration modes per action
    val getNextTrackVibrationMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[NEXT_TRACK_VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE
        }
    
    val getPreviousTrackVibrationMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PREVIOUS_TRACK_VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE
        }
    
    val getPlayPauseVibrationMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PLAY_PAUSE_VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE
        }
    
    // Combined flow for all custom vibration settings
    val getCustomVibrationSettings: Flow<Map<String, String>> = context.dataStore.data
        .map { preferences ->
            mapOf(
                "next_track_vibration_mode" to (preferences[NEXT_TRACK_VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE),
                "previous_track_vibration_mode" to (preferences[PREVIOUS_TRACK_VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE),
                "play_pause_vibration_mode" to (preferences[PLAY_PAUSE_VIBRATION_MODE] ?: DEFAULT_VIBRATION_MODE)
            )
        }

    // Flows for distinct pattern settings
    val getDistinctSettings: Flow<DistinctPatternSettings> = context.dataStore.data
        .map { preferences ->
            DistinctPatternSettings(
                nextTrackPulses = preferences[DISTINCT_NEXT_PULSES] ?: DEFAULT_DISTINCT_NEXT_PULSES,
                nextTrackIntensity = preferences[DISTINCT_NEXT_INTENSITY] ?: DEFAULT_DISTINCT_NEXT_INTENSITY,
                previousTrackPulses = preferences[DISTINCT_PREV_PULSES] ?: DEFAULT_DISTINCT_PREV_PULSES,
                previousTrackIntensity = preferences[DISTINCT_PREV_INTENSITY] ?: DEFAULT_DISTINCT_PREV_INTENSITY,
                playPausePulses = preferences[DISTINCT_PLAY_PULSES] ?: DEFAULT_DISTINCT_PLAY_PULSES,
                playPauseIntensity = preferences[DISTINCT_PLAY_INTENSITY] ?: DEFAULT_DISTINCT_PLAY_INTENSITY
            )
        }
    
    // Individual flows for distinct pattern settings
    val getDistinctNextPulses: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DISTINCT_NEXT_PULSES] ?: DEFAULT_DISTINCT_NEXT_PULSES
        }
    
    val getDistinctNextIntensity: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DISTINCT_NEXT_INTENSITY] ?: DEFAULT_DISTINCT_NEXT_INTENSITY
        }
    
    val getDistinctPrevPulses: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DISTINCT_PREV_PULSES] ?: DEFAULT_DISTINCT_PREV_PULSES
        }
    
    val getDistinctPrevIntensity: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DISTINCT_PREV_INTENSITY] ?: DEFAULT_DISTINCT_PREV_INTENSITY
        }
    
    val getDistinctPlayPulses: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DISTINCT_PLAY_PULSES] ?: DEFAULT_DISTINCT_PLAY_PULSES
        }
    
    val getDistinctPlayIntensity: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DISTINCT_PLAY_INTENSITY] ?: DEFAULT_DISTINCT_PLAY_INTENSITY
        }
      // Combined flow for distinct pattern settings
    val getDistinctPatternSettings: Flow<DistinctPatternSettings> = context.dataStore.data
        .map { preferences ->
            DistinctPatternSettings(
                nextTrackPulses = preferences[DISTINCT_NEXT_PULSES] ?: DEFAULT_DISTINCT_NEXT_PULSES,
                nextTrackIntensity = preferences[DISTINCT_NEXT_INTENSITY] ?: DEFAULT_DISTINCT_NEXT_INTENSITY,
                previousTrackPulses = preferences[DISTINCT_PREV_PULSES] ?: DEFAULT_DISTINCT_PREV_PULSES,
                previousTrackIntensity = preferences[DISTINCT_PREV_INTENSITY] ?: DEFAULT_DISTINCT_PREV_INTENSITY,
                playPausePulses = preferences[DISTINCT_PLAY_PULSES] ?: DEFAULT_DISTINCT_PLAY_PULSES,
                playPauseIntensity = preferences[DISTINCT_PLAY_INTENSITY] ?: DEFAULT_DISTINCT_PLAY_INTENSITY
            )
        }

    // Flows for advanced pattern settings
    val getAdvancedNextPattern: Flow<VibrationPattern> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[ADVANCED_NEXT_PATTERN]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<VibrationPattern>(jsonString)
                } catch (e: Exception) {
                    getDefaultAdvancedPattern()
                }
            } else {
                getDefaultAdvancedPattern()
            }
        }
    
    val getAdvancedPrevPattern: Flow<VibrationPattern> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[ADVANCED_PREV_PATTERN]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<VibrationPattern>(jsonString)
                } catch (e: Exception) {
                    getDefaultAdvancedPattern()
                }
            } else {
                getDefaultAdvancedPattern()
            }
        }
    
    val getAdvancedPlayPattern: Flow<VibrationPattern> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[ADVANCED_PLAY_PATTERN]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<VibrationPattern>(jsonString)
                } catch (e: Exception) {
                    getDefaultAdvancedPattern()
                }
            } else {
                getDefaultAdvancedPattern()
            }
        }
    
    // Combined flow for all advanced patterns
    val getAdvancedPatterns: Flow<Map<String, VibrationPattern>> = context.dataStore.data
        .map { preferences ->
            mapOf(
                "next_track_pattern" to (preferences[ADVANCED_NEXT_PATTERN]?.let { 
                    try { Json.decodeFromString<VibrationPattern>(it) } catch (e: Exception) { getDefaultAdvancedPattern() }
                } ?: getDefaultAdvancedPattern()),
                "previous_track_pattern" to (preferences[ADVANCED_PREV_PATTERN]?.let { 
                    try { Json.decodeFromString<VibrationPattern>(it) } catch (e: Exception) { getDefaultAdvancedPattern() }
                } ?: getDefaultAdvancedPattern()),
                "play_pause_pattern" to (preferences[ADVANCED_PLAY_PATTERN]?.let { 
                    try { Json.decodeFromString<VibrationPattern>(it) } catch (e: Exception) { getDefaultAdvancedPattern() }
                } ?: getDefaultAdvancedPattern())
            )
        }
    
    private fun getDefaultAdvancedPattern(): VibrationPattern {
        return VibrationPattern(
            pulses = listOf(VibrationPulse(duration = 50, amplitude = 255, pauseAfter = 0)),
            repeatCount = 0
        )
    }

    suspend fun setNextTrackThreshold(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[NEXT_TRACK_THRESHOLD] = value.coerceIn(MIN_THRESHOLD.toInt(), MAX_THRESHOLD.toInt())
        }
    }

    suspend fun setPreviousTrackThreshold(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PREVIOUS_TRACK_THRESHOLD] = value.coerceIn(MIN_THRESHOLD.toInt(), MAX_THRESHOLD.toInt())
        }
    }

    suspend fun setPlayPauseThreshold(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PLAY_PAUSE_THRESHOLD] = value.coerceIn(MIN_THRESHOLD.toInt(), MAX_THRESHOLD.toInt())
        }
    }

    //suspend fun setSimultaneousPressBuffer(value: Int) {
    //    context.dataStore.edit { preferences ->
    //        preferences[SIMULTANEOUS_PRESS_BUFFER] = value.coerceIn(MIN_BUFFER.toInt(), MAX_BUFFER.toInt())
    //    }
    //}

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setAppEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_ENABLED] = enabled
        }
    }    // Function to update debug monitoring state
    suspend fun setDebugMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEBUG_MONITORING_ENABLED] = enabled
        }
    }
      // Function to update vibration mode
    suspend fun setVibrationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_MODE] = mode
        }
    }
    
    // Functions to update custom vibration modes per action
    suspend fun setNextTrackVibrationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[NEXT_TRACK_VIBRATION_MODE] = mode
        }
    }
    
    suspend fun setPreviousTrackVibrationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PREVIOUS_TRACK_VIBRATION_MODE] = mode
        }
    }
    
    suspend fun setPlayPauseVibrationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAY_PAUSE_VIBRATION_MODE] = mode
        }
    }
    
    // Functions to update distinct pattern settings
    suspend fun setDistinctNextPulses(pulses: Int) {
        context.dataStore.edit { preferences ->
            preferences[DISTINCT_NEXT_PULSES] = pulses.coerceIn(1, 10)
        }
    }
    
    suspend fun setDistinctNextIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[DISTINCT_NEXT_INTENSITY] = intensity.coerceIn(1, 255)
        }
    }
    
    suspend fun setDistinctPrevPulses(pulses: Int) {
        context.dataStore.edit { preferences ->
            preferences[DISTINCT_PREV_PULSES] = pulses.coerceIn(1, 10)
        }
    }
    
    suspend fun setDistinctPrevIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[DISTINCT_PREV_INTENSITY] = intensity.coerceIn(1, 255)
        }
    }
    
    suspend fun setDistinctPlayPulses(pulses: Int) {
        context.dataStore.edit { preferences ->
            preferences[DISTINCT_PLAY_PULSES] = pulses.coerceIn(1, 10)
        }
    }
      suspend fun setDistinctPlayIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[DISTINCT_PLAY_INTENSITY] = intensity.coerceIn(1, 255)
        }
    }
    
    // Setters for advanced pattern settings
    suspend fun setAdvancedNextPattern(pattern: VibrationPattern) {
        context.dataStore.edit { preferences ->
            preferences[ADVANCED_NEXT_PATTERN] = Json.encodeToString(pattern)
        }
    }
    
    suspend fun setAdvancedPrevPattern(pattern: VibrationPattern) {
        context.dataStore.edit { preferences ->
            preferences[ADVANCED_PREV_PATTERN] = Json.encodeToString(pattern)
        }
    }
    
    suspend fun setAdvancedPlayPattern(pattern: VibrationPattern) {
        context.dataStore.edit { preferences ->
            preferences[ADVANCED_PLAY_PATTERN] = Json.encodeToString(pattern)
        }
    }
}
