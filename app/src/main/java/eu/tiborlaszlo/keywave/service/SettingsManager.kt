package eu.tiborlaszlo.keywave.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {    companion object {
        private val NEXT_TRACK_THRESHOLD = intPreferencesKey("next_track_threshold")
        private val PREVIOUS_TRACK_THRESHOLD = intPreferencesKey("previous_track_threshold")
        private val PLAY_PAUSE_THRESHOLD = intPreferencesKey("play_pause_threshold")
        private val SIMULTANEOUS_PRESS_BUFFER = intPreferencesKey("simultaneous_press_buffer")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        private val APP_ENABLED = booleanPreferencesKey("app_enabled")
        
        const val DEFAULT_NEXT_TRACK_THRESHOLD = 800L
        const val DEFAULT_PREVIOUS_TRACK_THRESHOLD = 800L
        const val DEFAULT_PLAY_PAUSE_THRESHOLD = 1000L
        const val DEFAULT_SIMULTANEOUS_BUFFER = 300L // 300ms buffer for simultaneous press
        const val MIN_THRESHOLD = 200L
        const val MAX_THRESHOLD = 2000L
        const val MIN_BUFFER = 100L
        const val MAX_BUFFER = 1000L
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
            preferences[HAPTIC_FEEDBACK_ENABLED] ?: true
        }
    
    val isAppEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APP_ENABLED] ?: true
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
    
    suspend fun setSimultaneousPressBuffer(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[SIMULTANEOUS_PRESS_BUFFER] = value.coerceIn(MIN_BUFFER.toInt(), MAX_BUFFER.toInt())
        }
    }
    
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
    
    suspend fun setAppEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_ENABLED] = enabled
        }
    }
}
