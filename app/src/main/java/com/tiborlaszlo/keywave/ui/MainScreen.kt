package com.tiborlaszlo.keywave.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiborlaszlo.keywave.data.ActionType
import com.tiborlaszlo.keywave.data.ActivationMode
import com.tiborlaszlo.keywave.data.CustomKeybind
import com.tiborlaszlo.keywave.data.HapticPattern
import com.tiborlaszlo.keywave.data.HardwareKey
import com.tiborlaszlo.keywave.data.KeybindStep
import com.tiborlaszlo.keywave.data.PressType
import com.tiborlaszlo.keywave.data.ScreenStateMode
import com.tiborlaszlo.keywave.data.SettingsState
import com.tiborlaszlo.keywave.ui.components.ActionChip
import com.tiborlaszlo.keywave.ui.components.ExpandableCard
import com.tiborlaszlo.keywave.ui.components.GlassCard
import com.tiborlaszlo.keywave.ui.components.KeyWaveSlider
import com.tiborlaszlo.keywave.ui.components.KeyWaveSwitch
import com.tiborlaszlo.keywave.ui.components.PillButton
import com.tiborlaszlo.keywave.ui.components.SegmentedControl
import com.tiborlaszlo.keywave.ui.components.SettingRow
import com.tiborlaszlo.keywave.ui.components.StatusPill
import com.tiborlaszlo.keywave.ui.theme.KeyWaveColors
import com.tiborlaszlo.keywave.ui.theme.KeyWaveTheme
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    settings: SettingsState,
    permissions: PermissionsStatus,
    onServiceToggle: (Boolean) -> Unit,
    onVolumeUpEnabledChange: (Boolean) -> Unit,
    onVolumeUpLongPressChange: (Long) -> Unit,
    onVolumeUpActionChange: (ActionType) -> Unit,
    onVolumeUpHapticPatternChange: (HapticPattern) -> Unit,
    onVolumeUpHapticIntensityChange: (Int) -> Unit,
    onVolumeUpCustomPulseCountChange: (Int) -> Unit,
    onVolumeUpCustomPulseMsChange: (Int) -> Unit,
    onVolumeUpCustomGapMsChange: (Int) -> Unit,
    onVolumeDownEnabledChange: (Boolean) -> Unit,
    onVolumeDownLongPressChange: (Long) -> Unit,
    onVolumeDownActionChange: (ActionType) -> Unit,
    onVolumeDownHapticPatternChange: (HapticPattern) -> Unit,
    onVolumeDownHapticIntensityChange: (Int) -> Unit,
    onVolumeDownCustomPulseCountChange: (Int) -> Unit,
    onVolumeDownCustomPulseMsChange: (Int) -> Unit,
    onVolumeDownCustomGapMsChange: (Int) -> Unit,
    onComboEnabledChange: (Boolean) -> Unit,
    onComboLongPressChange: (Long) -> Unit,
    onComboActionChange: (ActionType) -> Unit,
    onComboHapticPatternChange: (HapticPattern) -> Unit,
    onComboHapticIntensityChange: (Int) -> Unit,
    onComboCustomPulseCountChange: (Int) -> Unit,
    onComboCustomPulseMsChange: (Int) -> Unit,
    onComboCustomGapMsChange: (Int) -> Unit,
    onActivationModeChange: (ActivationMode) -> Unit,
    onScreenStateModeChange: (ScreenStateMode) -> Unit,
    onAllowlistChange: (Set<String>) -> Unit,
    onBlocklistChange: (Set<String>) -> Unit,
    onCustomKeybindsChange: (List<CustomKeybind>) -> Unit,
    onFeatureToggle: (FeatureToggle) -> Unit,
    onDebugToggle: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val extended = KeyWaveTheme.extendedColors
    val dimensions = KeyWaveTheme.dimensions
    val scope = rememberCoroutineScope()

    // Dialog states
    var showAllowlistDialog by remember { mutableStateOf(false) }
    var showBlocklistDialog by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf<ActionSheetType?>(null) }
    var showHapticSheet by remember { mutableStateOf<HapticSheetType?>(null) }
    var editingKeybind by remember { mutableStateOf<CustomKeybind?>(null) }

    // Expansion states
    var expandVolumeUp by remember { mutableStateOf(false) }
    var expandVolumeDown by remember { mutableStateOf(false) }
    var expandCombo by remember { mutableStateOf(false) }
    var expandKeybinds by remember { mutableStateOf(false) }
    var expandRules by remember { mutableStateOf(false) }
    
    // Hidden debug mode - tap status card 5 times to reveal
    var debugTapCount by remember { mutableIntStateOf(0) }
    var showDebugSection by remember { mutableStateOf(settings.debugEnabled) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (extended.isDark) {
                        listOf(KeyWaveColors.SpaceBlack, KeyWaveColors.DarkGray1)
                    } else {
                        listOf(KeyWaveColors.LightGray1, KeyWaveColors.White)
                    },
                ),
            ),
    ) {
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = dimensions.lg,
                vertical = dimensions.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(dimensions.md),
        ) {
            // ═══════════════════════════════════════════════════════════
            // Header & Service Toggle
            // ═══════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(20.dp))
                HeaderSection(
                    isServiceEnabled = settings.serviceEnabled,
                    onToggle = onServiceToggle,
                )
            }

            // ═══════════════════════════════════════════════════════════
            // Permission Status (tap 5 times to reveal debug mode)
            // ═══════════════════════════════════════════════════════════
            item {
                GlassCard(
                    onClick = {
                        if (!showDebugSection) {
                            debugTapCount++
                            val remaining = 5 - debugTapCount
                            val message = if (remaining > 0) {
                                "$remaining more taps to unlock debug mode"
                            } else {
                                showDebugSection = true
                                debugTapCount = 0
                                "Debug mode unlocked!"
                            }
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = androidx.compose.material3.SnackbarDuration.Indefinite,
                                    )
                                }
                                kotlinx.coroutines.delay(750)
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }
                    },
                ) {
                    Column {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = extended.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatusPill(
                                text = if (permissions.accessibilityEnabled) "Accessibility ✓" else "Accessibility ✗",
                                isOk = permissions.accessibilityEnabled,
                            )
                            StatusPill(
                                text = if (permissions.notificationListenerEnabled) "Notifications ✓" else "Notifications ✗",
                                isOk = permissions.notificationListenerEnabled,
                            )
                            StatusPill(
                                text = if (permissions.batteryOptimizationIgnored) "Battery ✓" else "Battery ✗",
                                isOk = permissions.batteryOptimizationIgnored,
                            )
                        }
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════
            // Hidden Debug Section
            // ═══════════════════════════════════════════════════════════
            if (showDebugSection) {
                item {
                    GlassCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "🔧 Debug Mode",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = extended.textPrimary,
                                )
                                Text(
                                    text = "Hide",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KeyWaveColors.Blue,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showDebugSection = false
                                            onDebugToggle(false)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enables detailed logging to logcat. Filter by 'KeyWave' tag.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Enable Debug Logging",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = extended.textPrimary,
                                )
                                KeyWaveSwitch(
                                    checked = settings.debugEnabled,
                                    onCheckedChange = { enabled ->
                                        onDebugToggle(enabled)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // Volume Up Configuration
            // ═══════════════════════════════════════════════════════════
            item {
                ButtonConfigCard(
                    title = "Volume Up",
                    emoji = "🔊",
                    enabled = settings.volumeUp.enabled,
                    action = settings.volumeUp.action,
                    longPressMs = settings.volumeUp.longPressMs,
                    hapticPattern = settings.volumeUp.hapticPattern,
                    hapticIntensity = settings.volumeUp.hapticIntensity,
                    customPulseCount = settings.volumeUp.customPulseCount,
                    customPulseMs = settings.volumeUp.customPulseMs,
                    customGapMs = settings.volumeUp.customGapMs,
                    expanded = expandVolumeUp,
                    onToggleExpand = { expandVolumeUp = !expandVolumeUp },
                    onEnabledChange = onVolumeUpEnabledChange,
                    onActionClick = { showActionSheet = ActionSheetType.VolumeUp },
                    onHapticClick = { showHapticSheet = HapticSheetType.VolumeUp },
                    onLongPressChange = onVolumeUpLongPressChange,
                    onIntensityChange = onVolumeUpHapticIntensityChange,
                    onCustomPulseCountChange = onVolumeUpCustomPulseCountChange,
                    onCustomPulseMsChange = onVolumeUpCustomPulseMsChange,
                    onCustomGapMsChange = onVolumeUpCustomGapMsChange,
                )
            }

            // ═══════════════════════════════════════════════════════════
            // Volume Down Configuration
            // ═══════════════════════════════════════════════════════════
            item {
                ButtonConfigCard(
                    title = "Volume Down",
                    emoji = "🔉",
                    enabled = settings.volumeDown.enabled,
                    action = settings.volumeDown.action,
                    longPressMs = settings.volumeDown.longPressMs,
                    hapticPattern = settings.volumeDown.hapticPattern,
                    hapticIntensity = settings.volumeDown.hapticIntensity,
                    customPulseCount = settings.volumeDown.customPulseCount,
                    customPulseMs = settings.volumeDown.customPulseMs,
                    customGapMs = settings.volumeDown.customGapMs,
                    expanded = expandVolumeDown,
                    onToggleExpand = { expandVolumeDown = !expandVolumeDown },
                    onEnabledChange = onVolumeDownEnabledChange,
                    onActionClick = { showActionSheet = ActionSheetType.VolumeDown },
                    onHapticClick = { showHapticSheet = HapticSheetType.VolumeDown },
                    onLongPressChange = onVolumeDownLongPressChange,
                    onIntensityChange = onVolumeDownHapticIntensityChange,
                    onCustomPulseCountChange = onVolumeDownCustomPulseCountChange,
                    onCustomPulseMsChange = onVolumeDownCustomPulseMsChange,
                    onCustomGapMsChange = onVolumeDownCustomGapMsChange,
                )
            }

            // ═══════════════════════════════════════════════════════════
            // Combo (Both Buttons) Configuration
            // ═══════════════════════════════════════════════════════════
            item {
                ButtonConfigCard(
                    title = "Both Buttons",
                    emoji = "🔊🔉",
                    enabled = settings.combo.enabled,
                    action = settings.combo.action,
                    longPressMs = settings.combo.longPressMs,
                    hapticPattern = settings.combo.hapticPattern,
                    hapticIntensity = settings.combo.hapticIntensity,
                    customPulseCount = settings.combo.customPulseCount,
                    customPulseMs = settings.combo.customPulseMs,
                    customGapMs = settings.combo.customGapMs,
                    expanded = expandCombo,
                    onToggleExpand = { expandCombo = !expandCombo },
                    onEnabledChange = onComboEnabledChange,
                    onActionClick = { showActionSheet = ActionSheetType.Combo },
                    onHapticClick = { showHapticSheet = HapticSheetType.Combo },
                    onLongPressChange = onComboLongPressChange,
                    onIntensityChange = onComboHapticIntensityChange,
                    onCustomPulseCountChange = onComboCustomPulseCountChange,
                    onCustomPulseMsChange = onComboCustomPulseMsChange,
                    onCustomGapMsChange = onComboCustomGapMsChange,
                )
            }

            // ═══════════════════════════════════════════════════════════
            // Activation Rules
            // ═══════════════════════════════════════════════════════════
            item {
                ExpandableCard(
                    title = "Activation Rules",
                    subtitle = "${labelForActivationMode(settings.activationMode)} • ${labelForScreenStateMode(settings.screenStateMode)}",
                    expanded = expandRules,
                    onToggle = { expandRules = !expandRules },
                ) {
                    Column {
                        Text(
                            text = "WHEN TO ACTIVATE",
                            style = MaterialTheme.typography.labelLarge,
                            color = extended.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SegmentedControl(
                            items = ActivationMode.entries,
                            selectedItem = settings.activationMode,
                            onItemSelected = onActivationModeChange,
                            itemLabel = { labelForActivationMode(it) },
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "SCREEN STATE",
                            style = MaterialTheme.typography.labelLarge,
                            color = extended.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SegmentedControl(
                            items = ScreenStateMode.entries,
                            selectedItem = settings.screenStateMode,
                            onItemSelected = onScreenStateModeChange,
                            itemLabel = { labelForScreenStateMode(it) },
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PillButton(
                                text = "Allowlist (${settings.allowlist.size})",
                                onClick = { showAllowlistDialog = true },
                                primary = false,
                                modifier = Modifier.weight(1f),
                            )
                            PillButton(
                                text = "Blocklist (${settings.blocklist.size})",
                                onClick = { showBlocklistDialog = true },
                                primary = false,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (settings.allowlist.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Allowlist is active. Blocklist is ignored.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeyWaveColors.Warning,
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // Custom Keybinds
            // ═══════════════════════════════════════════════════════════
            item {
                ExpandableCard(
                    title = "Custom Keybinds",
                    subtitle = "${settings.customKeybinds.size} configured",
                    expanded = expandKeybinds,
                    onToggle = { expandKeybinds = !expandKeybinds },
                    trailing = {
                        PillButton(
                            text = "+",
                            onClick = { editingKeybind = newDefaultKeybind() },
                            modifier = Modifier.size(36.dp),
                        )
                    },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (settings.customKeybinds.isEmpty()) {
                            Text(
                                text = "Create custom button sequences to trigger actions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = extended.textSecondary,
                            )
                        } else {
                            settings.customKeybinds.forEach { keybind ->
                                KeybindRow(
                                    keybind = keybind,
                                    onToggle = { enabled ->
                                        onCustomKeybindsChange(
                                            settings.customKeybinds.map {
                                                if (it.id == keybind.id) it.copy(enabled = enabled) else it
                                            },
                                        )
                                    },
                                    onEdit = { editingKeybind = keybind },
                                    onDelete = {
                                        onCustomKeybindsChange(
                                            settings.customKeybinds.filterNot { it.id == keybind.id },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Bottom Sheets & Dialogs
    // ═══════════════════════════════════════════════════════════════

    // Action Selection Sheet
    showActionSheet?.let { type ->
        ActionBottomSheet(
            currentAction = when (type) {
                ActionSheetType.VolumeUp -> settings.volumeUp.action
                ActionSheetType.VolumeDown -> settings.volumeDown.action
                ActionSheetType.Combo -> settings.combo.action
            },
            onSelect = { action ->
                when (type) {
                    ActionSheetType.VolumeUp -> onVolumeUpActionChange(action)
                    ActionSheetType.VolumeDown -> onVolumeDownActionChange(action)
                    ActionSheetType.Combo -> onComboActionChange(action)
                }
                showActionSheet = null
            },
            onDismiss = { showActionSheet = null },
        )
    }

    // Haptic Selection Sheet
    showHapticSheet?.let { type ->
        HapticBottomSheet(
            currentPattern = when (type) {
                HapticSheetType.VolumeUp -> settings.volumeUp.hapticPattern
                HapticSheetType.VolumeDown -> settings.volumeDown.hapticPattern
                HapticSheetType.Combo -> settings.combo.hapticPattern
            },
            onSelect = { pattern ->
                when (type) {
                    HapticSheetType.VolumeUp -> onVolumeUpHapticPatternChange(pattern)
                    HapticSheetType.VolumeDown -> onVolumeDownHapticPatternChange(pattern)
                    HapticSheetType.Combo -> onComboHapticPatternChange(pattern)
                }
                showHapticSheet = null
            },
            onDismiss = { showHapticSheet = null },
        )
    }

    // App Selection Dialogs
    if (showAllowlistDialog) {
        AppSelectionDialog(
            title = "Allowlist Apps",
            selected = settings.allowlist,
            onDismiss = { showAllowlistDialog = false },
            onSave = {
                onAllowlistChange(it)
                showAllowlistDialog = false
            },
            onClear = {
                onAllowlistChange(emptySet())
                showAllowlistDialog = false
            },
        )
    }

    if (showBlocklistDialog) {
        AppSelectionDialog(
            title = "Blocklist Apps",
            selected = settings.blocklist,
            onDismiss = { showBlocklistDialog = false },
            onSave = {
                onBlocklistChange(it)
                showBlocklistDialog = false
            },
            onClear = {
                onBlocklistChange(emptySet())
                showBlocklistDialog = false
            },
        )
    }

    // Keybind Editor
    editingKeybind?.let { keybind ->
        KeybindEditorDialog(
            keybind = keybind,
            onDismiss = { editingKeybind = null },
            onSave = { updated ->
                val updatedList = if (settings.customKeybinds.any { it.id == updated.id }) {
                    settings.customKeybinds.map { if (it.id == updated.id) updated else it }
                } else {
                    settings.customKeybinds + updated
                }
                onCustomKeybindsChange(updatedList)
                editingKeybind = null
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Header Section with Power Toggle
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeaderSection(
    isServiceEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors

    val glowAlpha by animateFloatAsState(
        targetValue = if (isServiceEnabled) 0.2f else 0f,
        animationSpec = tween(500),
        label = "headerGlow",
    )

    Box {
        // Glow effect
        if (isServiceEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KeyWaveColors.Blue.copy(alpha = 0.3f),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(KeyWaveTheme.dimensions.radiusLg),
                    ),
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KeyWave",
                        style = MaterialTheme.typography.displaySmall,
                        color = extended.textPrimary,
                    )
                    Text(
                        text = if (isServiceEnabled) "Active • Listening for gestures" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isServiceEnabled) KeyWaveColors.Success else extended.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Large toggle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isServiceEnabled) KeyWaveColors.Success else KeyWaveColors.DarkGray3,
                        )
                        .clickable { onToggle(!isServiceEnabled) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isServiceEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Button Configuration Card
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonConfigCard(
    title: String,
    emoji: String,
    enabled: Boolean,
    action: ActionType,
    longPressMs: Long,
    hapticPattern: HapticPattern,
    hapticIntensity: Int,
    customPulseCount: Int,
    customPulseMs: Int,
    customGapMs: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onActionClick: () -> Unit,
    onHapticClick: () -> Unit,
    onLongPressChange: (Long) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onCustomPulseCountChange: (Int) -> Unit,
    onCustomPulseMsChange: (Int) -> Unit,
    onCustomGapMsChange: (Int) -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors

    ExpandableCard(
        title = title,
        subtitle = if (enabled) labelForAction(action) else "Disabled",
        expanded = expanded,
        onToggle = onToggleExpand,
        trailing = {
            KeyWaveSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        },
    ) {
        Column {
            // Action Selection
            Text(
                text = "ACTION",
                style = MaterialTheme.typography.labelLarge,
                color = extended.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionType.entries.filter { it != ActionType.NONE }.forEach { actionType ->
                    ActionChip(
                        text = labelForAction(actionType),
                        selected = action == actionType,
                        onClick = { onActionClick() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Long Press Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Long Press",
                    style = MaterialTheme.typography.bodyLarge,
                    color = extended.textPrimary,
                )
                Text(
                    text = "${longPressMs}ms",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = KeyWaveColors.Blue,
                )
            }
            KeyWaveSlider(
                value = longPressMs.toFloat(),
                onValueChange = { onLongPressChange(it.roundToInt().toLong()) },
                valueRange = 200f..2000f,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Haptic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Haptic Feedback",
                        style = MaterialTheme.typography.bodyLarge,
                        color = extended.textPrimary,
                    )
                    Text(
                        text = labelForPattern(hapticPattern),
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary,
                    )
                }
                PillButton(
                    text = "Change",
                    onClick = onHapticClick,
                    primary = false,
                )
            }

            // Intensity slider
            if (hapticPattern != HapticPattern.OFF) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Intensity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary,
                    )
                    Text(
                        text = "$hapticIntensity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KeyWaveColors.Blue,
                    )
                }
                KeyWaveSlider(
                    value = hapticIntensity.toFloat(),
                    onValueChange = { onIntensityChange(it.roundToInt()) },
                    valueRange = 1f..255f,
                )
            }

            // Custom haptic controls
            AnimatedVisibility(
                visible = hapticPattern == HapticPattern.CUSTOM,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    SliderRow("Pulses", customPulseCount, 1..6) { onCustomPulseCountChange(it) }
                    SliderRow("Pulse Length", customPulseMs, 10..200, "ms") { onCustomPulseMsChange(it) }
                    SliderRow("Gap Length", customGapMs, 10..300, "ms") { onCustomGapMsChange(it) }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String = "",
    onChange: (Int) -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extended.textSecondary,
        )
        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.bodySmall,
            color = KeyWaveColors.Blue,
        )
    }
    KeyWaveSlider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
    )
}

// ═══════════════════════════════════════════════════════════════
// Keybind Row
// ═══════════════════════════════════════════════════════════════

@Composable
private fun KeybindRow(
    keybind: CustomKeybind,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors

    GlassCard(
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = keybind.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = extended.textPrimary,
                )
                Text(
                    text = "${labelForAction(keybind.action)} • ${keybind.steps.size} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Edit", color = KeyWaveColors.Blue)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = KeyWaveColors.Error)
                }
                KeyWaveSwitch(
                    checked = keybind.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Bottom Sheets
// ═══════════════════════════════════════════════════════════════

private enum class ActionSheetType { VolumeUp, VolumeDown, Combo }
private enum class HapticSheetType { VolumeUp, VolumeDown, Combo }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionBottomSheet(
    currentAction: ActionType,
    onSelect: (ActionType) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val extended = KeyWaveTheme.extendedColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (extended.isDark) KeyWaveColors.DarkGray1 else KeyWaveColors.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(extended.textTertiary),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Select Action",
                style = MaterialTheme.typography.headlineMedium,
                color = extended.textPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ActionType.entries.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(action) }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = labelForAction(action),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (action == currentAction) KeyWaveColors.Blue else extended.textPrimary,
                        fontWeight = if (action == currentAction) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (action == currentAction) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodyLarge,
                            color = KeyWaveColors.Blue,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HapticBottomSheet(
    currentPattern: HapticPattern,
    onSelect: (HapticPattern) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val extended = KeyWaveTheme.extendedColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (extended.isDark) KeyWaveColors.DarkGray1 else KeyWaveColors.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(extended.textTertiary),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Haptic Pattern",
                style = MaterialTheme.typography.headlineMedium,
                color = extended.textPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            HapticPattern.entries.forEach { pattern ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(pattern) }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = labelForPattern(pattern),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pattern == currentPattern) KeyWaveColors.Blue else extended.textPrimary,
                            fontWeight = if (pattern == currentPattern) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = descriptionForPattern(pattern),
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary,
                        )
                    }
                    if (pattern == currentPattern) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodyLarge,
                            color = KeyWaveColors.Blue,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// App Selection Dialog
// ═══════════════════════════════════════════════════════════════

private data class AppEntry(
    val label: String,
    val packageName: String,
)

@Composable
private fun AppSelectionDialog(
    title: String,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val extended = KeyWaveTheme.extendedColors
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var selection by remember(selected) { mutableStateOf(selected) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppEntry(pm.getApplicationLabel(it).toString(), it.packageName) }
            .sortedBy { it.label.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (extended.isDark) KeyWaveColors.DarkGray1 else KeyWaveColors.White,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = extended.textPrimary,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selection = if (selection.contains(app.packageName)) {
                                    selection - app.packageName
                                } else {
                                    selection + app.packageName
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selection.contains(app.packageName),
                            onCheckedChange = { checked ->
                                selection = if (checked) {
                                    selection + app.packageName
                                } else {
                                    selection - app.packageName
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = KeyWaveColors.Blue,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = extended.textPrimary,
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PillButton(
                text = "Save",
                onClick = { onSave(selection) },
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) {
                    Text("Clear All", color = KeyWaveColors.Error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = extended.textSecondary)
                }
            }
        },
    )
}

// ═══════════════════════════════════════════════════════════════
// Keybind Editor Dialog
// ═══════════════════════════════════════════════════════════════

@Composable
private fun KeybindEditorDialog(
    keybind: CustomKeybind,
    onDismiss: () -> Unit,
    onSave: (CustomKeybind) -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors

    var name by remember(keybind.id) { mutableStateOf(keybind.name) }
    var enabled by remember(keybind.id) { mutableStateOf(keybind.enabled) }
    var action by remember(keybind.id) { mutableStateOf(keybind.action) }
    var hapticPattern by remember(keybind.id) { mutableStateOf(keybind.hapticPattern) }
    var intensity by remember(keybind.id) { mutableIntStateOf(keybind.hapticIntensity) }
    var steps by remember(keybind.id) { mutableStateOf(keybind.steps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (extended.isDark) KeyWaveColors.DarkGray1 else KeyWaveColors.White,
        title = {
            Text(
                text = "Edit Keybind",
                style = MaterialTheme.typography.headlineMedium,
                color = extended.textPrimary,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = KeyWaveColors.Blue,
                        ),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enabled", color = extended.textPrimary)
                        KeyWaveSwitch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }

                item {
                    Text("Action", style = MaterialTheme.typography.labelLarge, color = extended.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        ActionType.entries.forEach { actionType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { action = actionType }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(labelForAction(actionType), color = extended.textPrimary)
                                RadioButton(
                                    selected = action == actionType,
                                    onClick = { action = actionType },
                                    colors = RadioButtonDefaults.colors(selectedColor = KeyWaveColors.Blue),
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Steps (${steps.size})", style = MaterialTheme.typography.labelLarge, color = extended.textSecondary)
                }

                items(steps.indices.toList()) { index ->
                    val step = steps[index]
                    GlassCard(contentPadding = PaddingValues(12.dp)) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Step ${index + 1}", fontWeight = FontWeight.SemiBold, color = extended.textPrimary)
                                if (steps.size > 1) {
                                    TextButton(onClick = {
                                        steps = steps.toMutableList().apply { removeAt(index) }
                                    }) {
                                        Text("Remove", color = KeyWaveColors.Error)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Key: ${labelForKey(step.key)}", color = extended.textSecondary)
                                TextButton(onClick = {
                                    val newKey = if (step.key == HardwareKey.VOLUME_UP) HardwareKey.VOLUME_DOWN else HardwareKey.VOLUME_UP
                                    steps = steps.toMutableList().apply { set(index, step.copy(key = newKey)) }
                                }) {
                                    Text("Toggle", color = KeyWaveColors.Blue)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Press: ${labelForPress(step.pressType)}", color = extended.textSecondary)
                                TextButton(onClick = {
                                    val newPress = if (step.pressType == PressType.SHORT) PressType.LONG else PressType.SHORT
                                    steps = steps.toMutableList().apply { set(index, step.copy(pressType = newPress)) }
                                }) {
                                    Text("Toggle", color = KeyWaveColors.Blue)
                                }
                            }
                        }
                    }
                }

                item {
                    if (steps.size < 4) {
                        PillButton(
                            text = "+ Add Step",
                            onClick = {
                                steps = steps + KeybindStep(
                                    key = HardwareKey.VOLUME_UP,
                                    pressType = PressType.SHORT,
                                    longPressMs = 600,
                                    delayAfterMs = 600,
                                )
                            },
                            primary = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            PillButton(
                text = "Save",
                onClick = {
                    onSave(
                        keybind.copy(
                            name = name.trim().ifEmpty { "Keybind" },
                            enabled = enabled,
                            action = action,
                            hapticPattern = hapticPattern,
                            hapticIntensity = intensity,
                            steps = steps,
                        ),
                    )
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = extended.textSecondary)
            }
        },
    )
}

// ═══════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════

private fun labelForActivationMode(mode: ActivationMode): String = when (mode) {
    ActivationMode.ALWAYS -> "Always"
    ActivationMode.MEDIA_ACTIVE -> "Media Active"
    ActivationMode.MEDIA_PLAYING -> "Playing"
}

private fun labelForScreenStateMode(mode: ScreenStateMode): String = when (mode) {
    ScreenStateMode.ANY -> "Any"
    ScreenStateMode.SCREEN_ON_ONLY -> "Screen On"
    ScreenStateMode.SCREEN_OFF_ONLY -> "Screen Off"
}

private fun labelForAction(action: ActionType): String = when (action) {
    ActionType.NEXT -> "Next Track"
    ActionType.PREVIOUS -> "Previous"
    ActionType.PLAY_PAUSE -> "Play/Pause"
    ActionType.STOP -> "Stop"
    ActionType.MUTE -> "Mute"
    ActionType.LAUNCH_APP -> "Launch App"
    ActionType.FLASHLIGHT -> "Flashlight"
    ActionType.ASSISTANT -> "Assistant"
    ActionType.SPOTIFY_LIKE -> "Spotify Like"
    ActionType.NONE -> "None"
}

private fun labelForPattern(pattern: HapticPattern): String = when (pattern) {
    HapticPattern.OFF -> "Off"
    HapticPattern.SHORT -> "Short"
    HapticPattern.LONG -> "Long"
    HapticPattern.DOUBLE -> "Double"
    HapticPattern.CUSTOM -> "Custom"
}

private fun descriptionForPattern(pattern: HapticPattern): String = when (pattern) {
    HapticPattern.OFF -> "No vibration"
    HapticPattern.SHORT -> "Quick tap"
    HapticPattern.LONG -> "Sustained vibration"
    HapticPattern.DOUBLE -> "Two quick pulses"
    HapticPattern.CUSTOM -> "Configure your own pattern"
}

private fun labelForKey(key: HardwareKey): String = when (key) {
    HardwareKey.VOLUME_UP -> "Vol Up"
    HardwareKey.VOLUME_DOWN -> "Vol Down"
}

private fun labelForPress(pressType: PressType): String = when (pressType) {
    PressType.SHORT -> "Short"
    PressType.LONG -> "Long"
}

private fun newDefaultKeybind(): CustomKeybind = CustomKeybind(
    id = UUID.randomUUID().toString(),
    name = "New Keybind",
    enabled = true,
    action = ActionType.PLAY_PAUSE,
    hapticPattern = HapticPattern.SHORT,
    hapticIntensity = 200,
    customPulseCount = 2,
    customPulseMs = 40,
    customGapMs = 60,
    steps = listOf(
        KeybindStep(
            key = HardwareKey.VOLUME_UP,
            pressType = PressType.LONG,
            longPressMs = 600,
            delayAfterMs = 600,
        ),
    ),
)

// ═══════════════════════════════════════════════════════════════
// Feature Toggle (preserved from original)
// ═══════════════════════════════════════════════════════════════

sealed class FeatureToggle {
    data class EnableNext(val enabled: Boolean) : FeatureToggle()
    data class EnablePrevious(val enabled: Boolean) : FeatureToggle()
    data class EnablePlayPause(val enabled: Boolean) : FeatureToggle()
    data class EnableStop(val enabled: Boolean) : FeatureToggle()
    data class EnableMute(val enabled: Boolean) : FeatureToggle()
    data class EnableLaunchApp(val enabled: Boolean) : FeatureToggle()
    data class EnableFlashlight(val enabled: Boolean) : FeatureToggle()
    data class EnableAssistant(val enabled: Boolean) : FeatureToggle()
    data class EnableSpotifyLike(val enabled: Boolean) : FeatureToggle()
}
