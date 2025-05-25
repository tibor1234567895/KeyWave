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
import androidx.lifecycle.lifecycleScope
import eu.tiborlaszlo.keywave.R
import eu.tiborlaszlo.keywave.service.SettingsManager
import eu.tiborlaszlo.keywave.ui.settings.SettingsScreen
import eu.tiborlaszlo.keywave.ui.theme.KeyWaveTheme
import eu.tiborlaszlo.keywave.utils.AccessibilityUtils
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set default exception handler for coroutines
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("KeyWave", "Uncaught exception", throwable)
            handler?.uncaughtException(thread, throwable)
        }
        
        enableEdgeToEdge()
        setContent {
            KeyWaveTheme {
                // Use Surface instead of Scaffold for better window attachment
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
    
    override fun onResume() {
        super.onResume()
        // Ensure window is properly attached
//        window?.decorView?.requestFitSystemWindows()
    }
    
    override fun onPause() {
        super.onPause()
        // Clean up any pending operations
        window?.decorView?.cancelPendingInputEvents()
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val isAppEnabled by settingsManager.isAppEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    
    var isAccessibilityEnabled by remember { 
        mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) 
    }
    
    // Monitor accessibility service state changes
    // Periodically check accessibility service state
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
            kotlinx.coroutines.delay(500) // Check every 500ms
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
                                !isAccessibilityEnabled -> "Accessibility Service Required"
                                isAppEnabled -> "Active"
                                else -> "Inactive"
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
                        enabled = true,
                        checked = isAppEnabled && isAccessibilityEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !isAccessibilityEnabled) {
                                // If trying to enable but accessibility service is not enabled
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } else {
                                // Normal toggle when accessibility service is enabled
                                scope.launch {
                                    settingsManager.setAppEnabled(enabled)
                                }
                            }
                        }
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
        SettingsScreen()
    }
}

