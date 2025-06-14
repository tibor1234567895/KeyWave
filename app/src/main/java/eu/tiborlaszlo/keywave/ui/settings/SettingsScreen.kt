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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import eu.tiborlaszlo.keywave.utils.VibrationManager
import eu.tiborlaszlo.keywave.utils.VibrationPattern
import eu.tiborlaszlo.keywave.utils.VibrationPulse
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val nextTrackThreshold by viewModel.nextTrackThreshold.collectAsState()
    val previousTrackThreshold by viewModel.previousTrackThreshold.collectAsState()
    val playPauseThreshold by viewModel.playPauseThreshold.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val vibrationMode by viewModel.vibrationMode.collectAsState()
    val debugMonitoringEnabled by viewModel.debugMonitoringEnabled.collectAsState()
      // Custom vibration mode states
    val nextTrackVibrationMode by viewModel.nextTrackVibrationMode.collectAsState()
    val previousTrackVibrationMode by viewModel.previousTrackVibrationMode.collectAsState()
    val playPauseVibrationMode by viewModel.playPauseVibrationMode.collectAsState()
    
    // Distinct pattern configuration states
    val distinctNextPulses by viewModel.distinctNextPulses.collectAsState()
    val distinctNextIntensity by viewModel.distinctNextIntensity.collectAsState()
    val distinctPrevPulses by viewModel.distinctPrevPulses.collectAsState()
    val distinctPrevIntensity by viewModel.distinctPrevIntensity.collectAsState()
    val distinctPlayPulses by viewModel.distinctPlayPulses.collectAsState()
    val distinctPlayIntensity by viewModel.distinctPlayIntensity.collectAsState()
    
    // Advanced pattern configuration states
    val advancedNextPattern by viewModel.advancedNextPattern.collectAsState()
    val advancedPrevPattern by viewModel.advancedPrevPattern.collectAsState()
    val advancedPlayPattern by viewModel.advancedPlayPattern.collectAsState()

    // Add a ScrollState
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Make the Column scrollable
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {        Text(
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

        VibrationModeSetting(
            currentMode = vibrationMode,
            onModeChange = viewModel::updateVibrationMode,
            enabled = hapticFeedbackEnabled
        )
          // Show custom vibration settings when CUSTOM mode is selected
        if (vibrationMode == "CUSTOM" && hapticFeedbackEnabled) {
            CustomVibrationSettings(
                nextTrackMode = nextTrackVibrationMode,
                previousTrackMode = previousTrackVibrationMode,
                playPauseMode = playPauseVibrationMode,
                onNextTrackModeChange = viewModel::updateNextTrackVibrationMode,
                onPreviousTrackModeChange = viewModel::updatePreviousTrackVibrationMode,
                onPlayPauseModeChange = viewModel::updatePlayPauseVibrationMode
            )
        }
        
        // Show distinct pattern configuration when DISTINCT mode is selected
        if (vibrationMode == "DISTINCT" && hapticFeedbackEnabled) {
            DistinctPatternSettings(
                nextPulses = distinctNextPulses,
                nextIntensity = distinctNextIntensity,
                prevPulses = distinctPrevPulses,
                prevIntensity = distinctPrevIntensity,
                playPulses = distinctPlayPulses,
                playIntensity = distinctPlayIntensity,
                onNextPulsesChange = viewModel::updateDistinctNextPulses,
                onNextIntensityChange = viewModel::updateDistinctNextIntensity,
                onPrevPulsesChange = viewModel::updateDistinctPrevPulses,
                onPrevIntensityChange = viewModel::updateDistinctPrevIntensity,
                onPlayPulsesChange = viewModel::updateDistinctPlayPulses,
                onPlayIntensityChange = viewModel::updateDistinctPlayIntensity
            )
        }
          // Show advanced pattern editor when ADVANCED mode is selected
        if (vibrationMode == "ADVANCED" && hapticFeedbackEnabled) {
            AdvancedPatternEditor(
                nextPattern = advancedNextPattern,
                prevPattern = advancedPrevPattern,
                playPattern = advancedPlayPattern,
                onNextPatternChange = viewModel::updateAdvancedNextPattern,
                onPrevPatternChange = viewModel::updateAdvancedPrevPattern,
                onPlayPatternChange = viewModel::updateAdvancedPlayPattern
            )
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VibrationModeSetting(
    currentMode: String,
    onModeChange: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val vibrationModes = VibrationManager.getAllVibrationModes()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_vibration_mode_title),
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        
        Text(
            text = stringResource(R.string.settings_vibration_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            TextField(
                value = VibrationManager.getVibrationModeDisplayName(currentMode),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {                vibrationModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(VibrationManager.getVibrationModeDisplayName(mode)) },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomVibrationSettings(
    nextTrackMode: String,
    previousTrackMode: String,
    playPauseMode: String,
    onNextTrackModeChange: (String) -> Unit,
    onPreviousTrackModeChange: (String) -> Unit,
    onPlayPauseModeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_custom_vibration_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(R.string.settings_custom_vibration_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Next Track vibration setting
        CustomActionVibrationSetting(
            actionName = stringResource(R.string.action_next_track),
            currentMode = nextTrackMode,
            onModeChange = onNextTrackModeChange
        )
        
        // Previous Track vibration setting
        CustomActionVibrationSetting(
            actionName = stringResource(R.string.action_previous_track),
            currentMode = previousTrackMode,
            onModeChange = onPreviousTrackModeChange
        )
        
        // Play/Pause vibration setting
        CustomActionVibrationSetting(
            actionName = stringResource(R.string.action_play_pause),
            currentMode = playPauseMode,
            onModeChange = onPlayPauseModeChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomActionVibrationSetting(
    actionName: String,
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val availableModes = VibrationManager.getAvailableCustomModes()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = actionName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = VibrationManager.getVibrationModeDisplayName(currentMode),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(VibrationManager.getVibrationModeDisplayName(mode)) },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DistinctPatternSettings(
    nextPulses: Int,
    nextIntensity: Int,
    prevPulses: Int,
    prevIntensity: Int,
    playPulses: Int,
    playIntensity: Int,
    onNextPulsesChange: (Int) -> Unit,
    onNextIntensityChange: (Int) -> Unit,
    onPrevPulsesChange: (Int) -> Unit,
    onPrevIntensityChange: (Int) -> Unit,
    onPlayPulsesChange: (Int) -> Unit,
    onPlayIntensityChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_distinct_config_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(R.string.settings_distinct_config_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Next Track configuration
        DistinctActionSettings(
            actionName = stringResource(R.string.action_next_track),
            pulses = nextPulses,
            intensity = nextIntensity,
            onPulsesChange = onNextPulsesChange,
            onIntensityChange = onNextIntensityChange
        )
        
        // Previous Track configuration
        DistinctActionSettings(
            actionName = stringResource(R.string.action_previous_track),
            pulses = prevPulses,
            intensity = prevIntensity,
            onPulsesChange = onPrevPulsesChange,
            onIntensityChange = onPrevIntensityChange
        )
        
        // Play/Pause configuration
        DistinctActionSettings(
            actionName = stringResource(R.string.action_play_pause),
            pulses = playPulses,
            intensity = playIntensity,
            onPulsesChange = onPlayPulsesChange,
            onIntensityChange = onPlayIntensityChange
        )
    }
}

@Composable
private fun DistinctActionSettings(
    actionName: String,
    pulses: Int,
    intensity: Int,
    onPulsesChange: (Int) -> Unit,
    onIntensityChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = actionName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Pulses setting
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Number of pulses",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$pulses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = pulses.toFloat(),
                onValueChange = { onPulsesChange(it.roundToInt()) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Intensity setting
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intensity",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(intensity * 100 / 255)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = intensity.toFloat(),
                onValueChange = { onIntensityChange(it.roundToInt()) },
                valueRange = 50f..255f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AdvancedPatternEditor(
    nextPattern: VibrationPattern,
    prevPattern: VibrationPattern,
    playPattern: VibrationPattern,
    onNextPatternChange: (VibrationPattern) -> Unit,
    onPrevPatternChange: (VibrationPattern) -> Unit,
    onPlayPatternChange: (VibrationPattern) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_advanced_patterns_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(R.string.settings_advanced_patterns_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Next Track pattern editor
        PatternEditor(
            title = stringResource(R.string.action_next_track),
            pattern = nextPattern,
            onPatternChange = onNextPatternChange
        )
        
        // Previous Track pattern editor
        PatternEditor(
            title = stringResource(R.string.action_previous_track),
            pattern = prevPattern,
            onPatternChange = onPrevPatternChange
        )
        
        // Play/Pause pattern editor
        PatternEditor(
            title = stringResource(R.string.action_play_pause),
            pattern = playPattern,
            onPatternChange = onPlayPatternChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternEditor(
    title: String,
    pattern: VibrationPattern,
    onPatternChange: (VibrationPattern) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Button(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Text(if (isExpanded) "Collapse" else "Edit Pattern")
                }
            }
            
            // Pattern summary
            Text(
                text = "Pulses: ${pattern.pulses.size}, Repeat: ${if (pattern.repeatCount == 0) "None" else if (pattern.repeatCount == -1) "Infinite" else pattern.repeatCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isExpanded) {
                // Repeat count setting
                RepeatCountSetting(
                    repeatCount = pattern.repeatCount,
                    onRepeatCountChange = { newRepeatCount ->
                        onPatternChange(pattern.copy(repeatCount = newRepeatCount))
                    }
                )
                
                // Pulses editor
                PulsesEditor(
                    pulses = pattern.pulses,
                    onPulsesChange = { newPulses ->
                        onPatternChange(pattern.copy(pulses = newPulses))
                    }
                )
            }
        }
    }
}

@Composable
private fun RepeatCountSetting(
    repeatCount: Int,
    onRepeatCountChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Repeat Pattern",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onRepeatCountChange(0) },
                modifier = Modifier.weight(1f)
            ) {
                Text("No Repeat")
            }
            
            Button(
                onClick = { onRepeatCountChange(1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Once")
            }
            
            Button(
                onClick = { onRepeatCountChange(-1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Infinite")
            }
        }
        
        if (repeatCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom repeat count:")
                
                OutlinedTextField(
                    value = repeatCount.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { intValue ->
                            if (intValue in 0..10) {
                                onRepeatCountChange(intValue)
                            }
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun PulsesEditor(
    pulses: List<VibrationPulse>,
    onPulsesChange: (List<VibrationPulse>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vibration Pulses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            FloatingActionButton(
                onClick = {
                    val newPulses = pulses + VibrationPulse()
                    onPulsesChange(newPulses)
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Pulse")
            }
        }
        
        pulses.forEachIndexed { index, pulse ->
            PulseEditor(
                pulse = pulse,
                onPulseChange = { newPulse ->
                    val newPulses = pulses.toMutableList()
                    newPulses[index] = newPulse
                    onPulsesChange(newPulses)
                },
                onDelete = if (pulses.size > 1) {
                    {
                        val newPulses = pulses.toMutableList()
                        newPulses.removeAt(index)
                        onPulsesChange(newPulses)
                    }
                } else null
            )
        }
    }
}

@Composable
private fun PulseEditor(
    pulse: VibrationPulse,
    onPulseChange: (VibrationPulse) -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pulse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Pulse")
                    }
                }
            }
            
            // Duration setting
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration: ${pulse.duration}ms")
                }
                
                Slider(
                    value = pulse.duration.toFloat(),
                    onValueChange = { value ->
                        onPulseChange(pulse.copy(duration = value.toLong()))
                    },
                    valueRange = 10f..500f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Amplitude setting
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Intensity: ${(pulse.amplitude * 100 / 255)}%")
                }
                
                Slider(
                    value = pulse.amplitude.toFloat(),
                    onValueChange = { value ->
                        onPulseChange(pulse.copy(amplitude = value.toInt()))
                    },
                    valueRange = 1f..255f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Pause after setting
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pause after: ${pulse.pauseAfter}ms")
                }
                
                Slider(
                    value = pulse.pauseAfter.toFloat(),
                    onValueChange = { value ->
                        onPulseChange(pulse.copy(pauseAfter = value.toLong()))
                    },
                    valueRange = 0f..1000f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
