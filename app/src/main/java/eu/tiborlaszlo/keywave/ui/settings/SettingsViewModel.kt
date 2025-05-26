package eu.tiborlaszlo.keywave.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.tiborlaszlo.keywave.service.SettingsManager
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
    }

    fun updateHapticFeedbackEnabled(enabled: Boolean) {
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
}
