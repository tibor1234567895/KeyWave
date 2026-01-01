package com.tiborlaszlo.keywave

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tiborlaszlo.keywave.core.ForegroundServiceController
import com.tiborlaszlo.keywave.data.DefaultSettings
import com.tiborlaszlo.keywave.data.SettingsRepository
import com.tiborlaszlo.keywave.ui.FeatureToggle
import com.tiborlaszlo.keywave.ui.MainScreen
import com.tiborlaszlo.keywave.ui.OnboardingScreen
import com.tiborlaszlo.keywave.ui.PermissionsChecker
import com.tiborlaszlo.keywave.ui.theme.KeyWaveTheme
import kotlinx.coroutines.launch

@Composable
fun KeyWaveApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = DefaultSettings.state)

    var permissions by remember { mutableStateOf(PermissionsChecker.check(context)) }
    var onboardingComplete by remember { mutableStateOf(false) }
    
    // Refresh permissions on resume AND periodically while on onboarding screen
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                permissions = PermissionsChecker.check(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Also refresh permissions every 500ms while on onboarding (catches delayed registration)
    if (!onboardingComplete) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(500)
                permissions = PermissionsChecker.check(context)
            }
        }
    }

    KeyWaveTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedContent(
                targetState = onboardingComplete && permissions.requiredGranted,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 })
                        .togetherWith(fadeOut() + slideOutVertically { -it / 4 })
                },
                label = "screenTransition",
            ) { showMainScreen ->
                if (showMainScreen) {
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(settings.serviceEnabled) {
                        if (settings.serviceEnabled) {
                            ForegroundServiceController.start(context)
                        } else {
                            ForegroundServiceController.stop(context)
                        }
                    }
                    MainScreen(
                        settings = settings,
                        permissions = permissions,
                        onServiceToggle = { enabled ->
                            scope.launch { repository.setServiceEnabled(enabled) }
                        },
                        onVolumeUpEnabledChange = { enabled ->
                            scope.launch { repository.setVolumeUpEnabled(enabled) }
                        },
                        onVolumeUpLongPressChange = { value ->
                            scope.launch { repository.setVolumeUpLongPressMs(value) }
                        },
                        onVolumeUpActionChange = { action ->
                            scope.launch { repository.setVolumeUpAction(action) }
                        },
                        onVolumeUpHapticPatternChange = { pattern ->
                            scope.launch { repository.setVolumeUpHapticPattern(pattern) }
                        },
                        onVolumeUpHapticIntensityChange = { value ->
                            scope.launch { repository.setVolumeUpHapticIntensity(value) }
                        },
                        onVolumeUpCustomPulseCountChange = { value ->
                            scope.launch { repository.setVolumeUpCustomPulseCount(value) }
                        },
                        onVolumeUpCustomPulseMsChange = { value ->
                            scope.launch { repository.setVolumeUpCustomPulseMs(value) }
                        },
                        onVolumeUpCustomGapMsChange = { value ->
                            scope.launch { repository.setVolumeUpCustomGapMs(value) }
                        },
                        onVolumeDownEnabledChange = { enabled ->
                            scope.launch { repository.setVolumeDownEnabled(enabled) }
                        },
                        onVolumeDownLongPressChange = { value ->
                            scope.launch { repository.setVolumeDownLongPressMs(value) }
                        },
                        onVolumeDownActionChange = { action ->
                            scope.launch { repository.setVolumeDownAction(action) }
                        },
                        onVolumeDownHapticPatternChange = { pattern ->
                            scope.launch { repository.setVolumeDownHapticPattern(pattern) }
                        },
                        onVolumeDownHapticIntensityChange = { value ->
                            scope.launch { repository.setVolumeDownHapticIntensity(value) }
                        },
                        onVolumeDownCustomPulseCountChange = { value ->
                            scope.launch { repository.setVolumeDownCustomPulseCount(value) }
                        },
                        onVolumeDownCustomPulseMsChange = { value ->
                            scope.launch { repository.setVolumeDownCustomPulseMs(value) }
                        },
                        onVolumeDownCustomGapMsChange = { value ->
                            scope.launch { repository.setVolumeDownCustomGapMs(value) }
                        },
                        onComboEnabledChange = { enabled ->
                            scope.launch { repository.setComboEnabled(enabled) }
                        },
                        onComboLongPressChange = { value ->
                            scope.launch { repository.setComboLongPressMs(value) }
                        },
                        onComboActionChange = { action ->
                            scope.launch { repository.setComboAction(action) }
                        },
                        onComboHapticPatternChange = { pattern ->
                            scope.launch { repository.setComboHapticPattern(pattern) }
                        },
                        onComboHapticIntensityChange = { value ->
                            scope.launch { repository.setComboHapticIntensity(value) }
                        },
                        onComboCustomPulseCountChange = { value ->
                            scope.launch { repository.setComboCustomPulseCount(value) }
                        },
                        onComboCustomPulseMsChange = { value ->
                            scope.launch { repository.setComboCustomPulseMs(value) }
                        },
                        onComboCustomGapMsChange = { value ->
                            scope.launch { repository.setComboCustomGapMs(value) }
                        },
                        onActivationModeChange = { mode ->
                            scope.launch { repository.setActivationMode(mode) }
                        },
                        onScreenStateModeChange = { mode ->
                            scope.launch { repository.setScreenStateMode(mode) }
                        },
                        onAllowlistChange = { items ->
                            scope.launch { repository.setAllowlist(items) }
                        },
                        onBlocklistChange = { items ->
                            scope.launch { repository.setBlocklist(items) }
                        },
                        onCustomKeybindsChange = { keybinds ->
                            scope.launch { repository.setCustomKeybinds(keybinds) }
                        },
                        onFeatureToggle = { toggle ->
                            scope.launch {
                                when (toggle) {
                                    is FeatureToggle.EnableNext -> repository.setEnableNext(toggle.enabled)
                                    is FeatureToggle.EnablePrevious -> repository.setEnablePrevious(toggle.enabled)
                                    is FeatureToggle.EnablePlayPause -> repository.setEnablePlayPause(toggle.enabled)
                                    is FeatureToggle.EnableStop -> repository.setEnableStop(toggle.enabled)
                                    is FeatureToggle.EnableMute -> repository.setEnableMute(toggle.enabled)
                                    is FeatureToggle.EnableLaunchApp -> repository.setEnableLaunchApp(toggle.enabled)
                                    is FeatureToggle.EnableFlashlight -> repository.setEnableFlashlight(toggle.enabled)
                                    is FeatureToggle.EnableAssistant -> repository.setEnableAssistant(toggle.enabled)
                                    is FeatureToggle.EnableSpotifyLike -> repository.setEnableSpotifyLike(toggle.enabled)
                                }
                            }
                        },
                        onDebugToggle = { enabled ->
                            scope.launch { repository.setDebugEnabled(enabled) }
                        },
                    )
                } else {
                    OnboardingScreen(
                        context = context, 
                        permissions = permissions,
                        onContinue = { onboardingComplete = true },
                    )
                }
            }
        }
    }
}
