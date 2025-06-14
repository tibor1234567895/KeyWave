package eu.tiborlaszlo.keywave.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.tiborlaszlo.keywave.service.SettingsManager
import eu.tiborlaszlo.keywave.utils.DistinctPatternSettings
import eu.tiborlaszlo.keywave.utils.VibrationPattern
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    //val isAppEnabled: StateFlow<Boolean> = settingsManager.isAppEnabled
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val nextTrackThreshold: StateFlow<Long> = settingsManager.getNextTrackThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_NEXT_TRACK_THRESHOLD)
    val previousTrackThreshold: StateFlow<Long> = settingsManager.getPreviousTrackThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_PREVIOUS_TRACK_THRESHOLD)
    
    val playPauseThreshold: StateFlow<Long> = settingsManager.getPlayPauseThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_PLAY_PAUSE_THRESHOLD)

    val hapticFeedbackEnabled: StateFlow<Boolean> = settingsManager.getHapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
      val vibrationMode: StateFlow<String> = settingsManager.getVibrationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_VIBRATION_MODE)
    
    // StateFlows for custom vibration modes per action
    val nextTrackVibrationMode: StateFlow<String> = settingsManager.getNextTrackVibrationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_VIBRATION_MODE)
    
    val previousTrackVibrationMode: StateFlow<String> = settingsManager.getPreviousTrackVibrationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_VIBRATION_MODE)
    
    val playPauseVibrationMode: StateFlow<String> = settingsManager.getPlayPauseVibrationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_VIBRATION_MODE)
      val customVibrationSettings: StateFlow<Map<String, String>> = settingsManager.getCustomVibrationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
    
    // StateFlows for distinct pattern settings
    val distinctSettings: StateFlow<DistinctPatternSettings> = settingsManager.getDistinctSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DistinctPatternSettings())
    
    val distinctNextPulses: StateFlow<Int> = settingsManager.getDistinctNextPulses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_DISTINCT_NEXT_PULSES)
    
    val distinctNextIntensity: StateFlow<Int> = settingsManager.getDistinctNextIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_DISTINCT_NEXT_INTENSITY)
    
    val distinctPrevPulses: StateFlow<Int> = settingsManager.getDistinctPrevPulses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_DISTINCT_PREV_PULSES)
    
    val distinctPrevIntensity: StateFlow<Int> = settingsManager.getDistinctPrevIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_DISTINCT_PREV_INTENSITY)
    
    val distinctPlayPulses: StateFlow<Int> = settingsManager.getDistinctPlayPulses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_DISTINCT_PLAY_PULSES)
      val distinctPlayIntensity: StateFlow<Int> = settingsManager.getDistinctPlayIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsManager.DEFAULT_DISTINCT_PLAY_INTENSITY)

    // StateFlows for advanced pattern settings
    val advancedNextPattern: StateFlow<VibrationPattern> = settingsManager.getAdvancedNextPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), VibrationPattern())
    
    val advancedPrevPattern: StateFlow<VibrationPattern> = settingsManager.getAdvancedPrevPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), VibrationPattern())
    
    val advancedPlayPattern: StateFlow<VibrationPattern> = settingsManager.getAdvancedPlayPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), VibrationPattern())

    // StateFlow for debug monitoring
    val debugMonitoringEnabled: StateFlow<Boolean> = settingsManager.getDebugMonitoringEnabled
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            SettingsManager.DEFAULT_DEBUG_MONITORING_ENABLED
        )


    fun updateNextTrackThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setNextTrackThreshold(value)
        }
    }

    fun updatePreviousTrackThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setPreviousTrackThreshold(value)
        }
    }

    fun updatePlayPauseThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setPlayPauseThreshold(value)
        }
    }    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setHapticFeedbackEnabled(enabled)
        }
    }
    
    // Function to update debug monitoring
    fun updateDebugMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDebugMonitoringEnabled(enabled)
        }
    }
    
    // Function to update vibration mode
    fun updateVibrationMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setVibrationMode(mode)
        }
    }
    
    // Functions to update custom vibration modes per action
    fun updateNextTrackVibrationMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setNextTrackVibrationMode(mode)
        }
    }
    
    fun updatePreviousTrackVibrationMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setPreviousTrackVibrationMode(mode)
        }
    }
    
    fun updatePlayPauseVibrationMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setPlayPauseVibrationMode(mode)
        }
    }

    // Functions to update distinct pattern settings
    fun updateDistinctNextPulses(pulses: Int) {
        viewModelScope.launch {
            settingsManager.setDistinctNextPulses(pulses)
        }
    }
    
    fun updateDistinctNextIntensity(intensity: Int) {
        viewModelScope.launch {
            settingsManager.setDistinctNextIntensity(intensity)
        }
    }
    
    fun updateDistinctPrevPulses(pulses: Int) {
        viewModelScope.launch {
            settingsManager.setDistinctPrevPulses(pulses)
        }
    }
    
    fun updateDistinctPrevIntensity(intensity: Int) {
        viewModelScope.launch {
            settingsManager.setDistinctPrevIntensity(intensity)
        }
    }
    
    fun updateDistinctPlayPulses(pulses: Int) {
        viewModelScope.launch {
            settingsManager.setDistinctPlayPulses(pulses)
        }
    }
      fun updateDistinctPlayIntensity(intensity: Int) {
        viewModelScope.launch {
            settingsManager.setDistinctPlayIntensity(intensity)
        }
    }
    
    // Functions to update advanced pattern settings
    fun updateAdvancedNextPattern(pattern: VibrationPattern) {
        viewModelScope.launch {
            settingsManager.setAdvancedNextPattern(pattern)
        }
    }
    
    fun updateAdvancedPrevPattern(pattern: VibrationPattern) {
        viewModelScope.launch {
            settingsManager.setAdvancedPrevPattern(pattern)
        }
    }
    
    fun updateAdvancedPlayPattern(pattern: VibrationPattern) {
        viewModelScope.launch {
            settingsManager.setAdvancedPlayPattern(pattern)
        }
    }
}
