package eu.tiborlaszlo.keywave

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eu.tiborlaszlo.keywave.service.SettingsManager
import eu.tiborlaszlo.keywave.ui.settings.SettingsScreen
import eu.tiborlaszlo.keywave.ui.theme.KeyWaveTheme
import eu.tiborlaszlo.keywave.utils.AccessibilityUtils
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set default exception handler for coroutines
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e(
                "KeyWaveApp",
                "Uncaught exception in thread: ${thread.name}",
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()
        setContent {
            KeyWaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                        MainScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val isAppEnabled by settingsManager.isAppEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope() // For launching coroutines from UI events

    // State for accessibility service status
    var isAccessibilityEnabled by remember {
        mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context))
    }

    // Activity reference to access lifecycle
    val activity = LocalContext.current as? ComponentActivity

    // LaunchedEffect to update accessibility status when the activity resumes
    // and periodically while it's resumed.
    LaunchedEffect(key1 = Unit, key2 = activity) {
        activity?.lifecycleScope?.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // This block will now execute when the activity is RESUMED
                // and cancel when it's PAUSED.
                while (isActive) { // isActive is from the coroutine scope
                    val currentStatus = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                    if (isAccessibilityEnabled != currentStatus) {
                        isAccessibilityEnabled = currentStatus
                        android.util.Log.d(
                            "MainScreen",
                            "Accessibility service status updated: $currentStatus"
                        )
                    }
                    delay(1000) // Check every 1 second while resumed
                }
            }
        }
        // Initial check in case repeatOnLifecycle doesn't run immediately or if not in RESUMED state yet
        if (!isAccessibilityEnabled) {
            isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Global app toggle section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KeyWave",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = when {
                                !isAccessibilityEnabled -> stringResource(R.string.accessibility_required_short) // "Accessibility Needed"
                                isAppEnabled -> stringResource(R.string.service_status_active) // "Active"
                                else -> stringResource(R.string.service_status_inactive) // "Inactive"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                !isAccessibilityEnabled -> MaterialTheme.colorScheme.error
                                isAppEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Switch(
                        checked = isAppEnabled && isAccessibilityEnabled,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState && !isAccessibilityEnabled) {
                                // Trying to enable, but accessibility is off. Guide user.
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                // The switch will reflect actual state once accessibility is enabled and app is toggled on.
                                // We don't update isAppEnabled in settingsManager here,
                                // as the primary issue is accessibility.
                            } else if (isAccessibilityEnabled) {
                                // Accessibility is enabled, so toggle app's own enabled state.
                                scope.launch {
                                    settingsManager.setAppEnabled(newCheckedState)
                                }
                            }
                            // If trying to disable while accessibility is off, it should just reflect visually.
                            // The actual app state (isAppEnabled) is what matters for the service.
                        },
                        // The switch is enabled for interaction if accessibility is on,
                        // OR if the user is trying to turn it ON (which would guide them to settings).
                        // If accessibility is OFF and the switch is also OFF, it means the app is "off"
                        // and trying to turn it on should take them to settings.
                        enabled = true // Always allow interaction; logic inside onCheckedChange handles guidance.
                    )
                }

                Text(
                    text = stringResource(R.string.service_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isAccessibilityEnabled) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.accessibility_required),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.accessibility_explanation),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.open_accessibility_settings))
                            }
                        }
                    }
                }
            }
        }

        // Settings section
        SettingsScreen() // Assuming SettingsScreen has its own ViewModel and state handling
    }
}
