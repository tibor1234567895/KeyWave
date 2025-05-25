package eu.tiborlaszlo.keywave.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdSetting(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {    var textValue by rememberSaveable(value) { mutableStateOf(value.toString()) }
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
            Column {
                Text(
                    text = "Haptic Feedback",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Vibrate when performing media actions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}
