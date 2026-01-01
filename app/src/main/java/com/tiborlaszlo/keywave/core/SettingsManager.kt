package com.tiborlaszlo.keywave.core

import android.content.Context
import com.tiborlaszlo.keywave.data.DefaultSettings
import com.tiborlaszlo.keywave.data.SettingsRepository
import com.tiborlaszlo.keywave.data.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsManager(context: Context, scope: CoroutineScope) {
  private val repository = SettingsRepository(context)
  private val _state = MutableStateFlow(DefaultSettings.state)
  val state: StateFlow<SettingsState> = _state.asStateFlow()

  init {
    scope.launch {
      repository.settings.collect { latest ->
        _state.value = latest
      }
    }
  }
}
