package eu.tiborlaszlo.keywave.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.tiborlaszlo.keywave.R
import eu.tiborlaszlo.keywave.service.SettingsManager
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val nextTrackThreshold by viewModel.nextTrackThreshold.collectAsState()
    val previousTrackThreshold by viewModel.previousTrackThreshold.collectAsState()
    val playPauseThreshold by viewModel.playPauseThreshold.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val debugMonitoringEnabled by viewModel.debugMonitoringEnabled.collectAsState()

    // Add a ScrollState
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Make the Column scrollable
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings", // Consider using stringResource(R.string.settings_title)
            style = MaterialTheme.typography.headlineMedium
        )

        ThresholdSetting(
            title = "Next Track Threshold",
            value = nextTrackThreshold.toInt(),
            onValueChange = viewModel::updateNextTrackThreshold
        )

        ThresholdSetting(
            title = "Previous Track Threshold",
            value = previousTrackThreshold.toInt(),
            onValueChange = viewModel::updatePreviousTrackThreshold
        )

        ThresholdSetting(
            title = "Play/Pause Threshold",
            value = playPauseThreshold.toInt(),
            onValueChange = viewModel::updatePlayPauseThreshold
        )

        HapticFeedbackSetting(
            enabled = hapticFeedbackEnabled,
            onEnabledChange = viewModel::updateHapticFeedbackEnabled
        )

        DebugMonitoringSetting(
            enabled = debugMonitoringEnabled,
            onEnabledChange = viewModel::updateDebugMonitoringEnabled
        )

        // Add some extra space at the bottom if needed for better scrollability
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdSetting(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var textValue by rememberSaveable(value) { mutableStateOf(value.toString()) }
    var sliderValue by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }
    var isError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    newValue.toIntOrNull()?.let { intValue ->
                        if (intValue in SettingsManager.MIN_THRESHOLD..SettingsManager.MAX_THRESHOLD) {
                            isError = false
                            sliderValue = intValue.toFloat()
                            onValueChange(intValue)
                        } else {
                            isError = true
                        }
                    } ?: run {
                        isError = true
                    }
                },
                modifier = Modifier.width(120.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = { Text("ms") },
                isError = isError,
                supportingText = if (isError) {
                    { Text("Enter ${SettingsManager.MIN_THRESHOLD}-${SettingsManager.MAX_THRESHOLD}") }
                } else null
            )

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    // Round to nearest 50ms
                    val roundedValue = (newValue / 50f).roundToInt() * 50
                    sliderValue = roundedValue.toFloat()
                    textValue = roundedValue.toString()
                    onValueChange(roundedValue)
                },
                valueRange = SettingsManager.MIN_THRESHOLD.toFloat()..SettingsManager.MAX_THRESHOLD.toFloat(),
                steps = ((SettingsManager.MAX_THRESHOLD - SettingsManager.MIN_THRESHOLD) / 50).toInt(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HapticFeedbackSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) { // Allow text to take space and wrap if needed
                Text(
                    text = "Haptic Feedback", // Consider stringResource
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Vibrate when performing media actions", // Consider stringResource
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp)) // Add some space before the switch
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

// New Composable for Debug Monitoring Setting
@Composable
private fun DebugMonitoringSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_debug_monitoring_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_debug_monitoring_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}
