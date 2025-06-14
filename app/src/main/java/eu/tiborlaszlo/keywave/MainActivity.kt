package eu.tiborlaszlo.keywave

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import eu.tiborlaszlo.keywave.utils.NotificationListenerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    
    // State for notification listener service status
    var isNotificationListenerEnabled by remember {
        mutableStateOf(NotificationListenerUtils.isNotificationListenerEnabled(context))
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
                    val currentAccessibilityStatus = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                    val currentNotificationListenerStatus = NotificationListenerUtils.isNotificationListenerEnabled(context)
                    
                    if (isAccessibilityEnabled != currentAccessibilityStatus) {
                        isAccessibilityEnabled = currentAccessibilityStatus
                        android.util.Log.d(
                            "MainScreen",
                            "Accessibility service status updated: $currentAccessibilityStatus"
                        )
                    }
                    
                    if (isNotificationListenerEnabled != currentNotificationListenerStatus) {
                        isNotificationListenerEnabled = currentNotificationListenerStatus
                        android.util.Log.d(
                            "MainScreen",
                            "Notification listener status updated: $currentNotificationListenerStatus"
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
        if (!isNotificationListenerEnabled) {
            isNotificationListenerEnabled = NotificationListenerUtils.isNotificationListenerEnabled(context)
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
                                !isAccessibilityEnabled -> stringResource(R.string.accessibility_required_short)
                                !isNotificationListenerEnabled -> stringResource(R.string.notification_access_needed_short)
                                isAppEnabled -> stringResource(R.string.service_status_active)
                                else -> stringResource(R.string.service_status_inactive)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                !isAccessibilityEnabled || !isNotificationListenerEnabled -> MaterialTheme.colorScheme.error
                                isAppEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Switch(
                        checked = isAppEnabled && isAccessibilityEnabled && isNotificationListenerEnabled,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState && !isAccessibilityEnabled) {
                                // Need accessibility permission first
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } else if (newCheckedState && !isNotificationListenerEnabled) {
                                // Need notification listener permission
                                NotificationListenerUtils.openNotificationListenerSettings(context)
                            } else if (isAccessibilityEnabled && isNotificationListenerEnabled) {
                                // Both permissions available, toggle app state
                                scope.launch {
                                    settingsManager.setAppEnabled(newCheckedState)
                                }
                            }
                        },
                        enabled = true
                    )
                }

                Text(
                    text = stringResource(R.string.service_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isAccessibilityEnabled || !isNotificationListenerEnabled) {
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
                            if (!isAccessibilityEnabled) {
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
                            
                            if (!isNotificationListenerEnabled) {
                                if (!isAccessibilityEnabled) {
                                    // Add spacing between the two permission sections
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Text(
                                    text = stringResource(R.string.notification_access_required),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = stringResource(R.string.notification_access_explanation),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                FilledTonalButton(
                                    onClick = {
                                        NotificationListenerUtils.openNotificationListenerSettings(context)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(stringResource(R.string.open_notification_settings))
                                }
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
