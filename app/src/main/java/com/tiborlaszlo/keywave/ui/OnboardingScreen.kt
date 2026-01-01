package com.tiborlaszlo.keywave.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiborlaszlo.keywave.ui.components.GlassCard
import com.tiborlaszlo.keywave.ui.components.PillButton
import com.tiborlaszlo.keywave.ui.theme.KeyWaveColors
import com.tiborlaszlo.keywave.ui.theme.KeyWaveTheme
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    context: Context,
    permissions: PermissionsStatus,
    onContinue: () -> Unit = {},
) {
    val extended = KeyWaveTheme.extendedColors
    val dimensions = KeyWaveTheme.dimensions

    // Staggered animation states
    var showHero by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }
    
    // State for showing disclosure dialogs
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    var showNotificationDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showHero = true
        delay(200)
        showCards = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (extended.isDark) {
                        listOf(
                            KeyWaveColors.SpaceBlack,
                            KeyWaveColors.DarkGray1,
                        )
                    } else {
                        listOf(
                            KeyWaveColors.LightGray1,
                            KeyWaveColors.White,
                        )
                    },
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensions.xl),
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Hero Section
            AnimatedVisibility(
                visible = showHero,
                enter = fadeIn(tween(500)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(500),
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // App Icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.linearGradient(KeyWaveColors.GradientPrimary),
                                shape = RoundedCornerShape(24.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 44.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Welcome to KeyWave",
                        style = MaterialTheme.typography.displayMedium,
                        color = extended.textPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Control your music with volume buttons.\nFast, private, and always ready.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = extended.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Permission Cards
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(tween(400)),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensions.md),
                ) {
                    PermissionCard(
                        title = "Accessibility",
                        description = "Detect volume button presses system-wide",
                        granted = permissions.accessibilityEnabled,
                        onAction = {
                            // Show the prominent disclosure dialog first
                            showAccessibilityDisclosure = true
                        },
                        animationDelay = 0,
                    )

                    PermissionCard(
                        title = "Notifications",
                        description = "Find the active media player",
                        granted = permissions.notificationListenerEnabled,
                        onAction = {
                            // Show the prominent disclosure dialog first
                            showNotificationDisclosure = true
                        },
                        animationDelay = 100,
                    )

                    PermissionCard(
                        title = "Battery",
                        description = "Keep KeyWave running reliably",
                        granted = permissions.batteryOptimizationIgnored,
                        onAction = {
                            PermissionLauncher.safeStartOrToast(
                                context,
                                PermissionIntents.batteryOptimizationSettings(context),
                            )
                        },
                        animationDelay = 200,
                        optional = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress Indicator
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(tween(600)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Only count the 2 REQUIRED permissions (accessibility + notifications)
                    val requiredGrantedCount = listOf(
                        permissions.accessibilityEnabled,
                        permissions.notificationListenerEnabled,
                    ).count { it }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(2) { index ->
                            val isActive = index < requiredGrantedCount
                            val dotScale by animateFloatAsState(
                                targetValue = if (isActive) 1f else 0.7f,
                                label = "dotScale",
                            )

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) KeyWaveColors.Success
                                        else if (extended.isDark) KeyWaveColors.DarkGray4
                                        else KeyWaveColors.LightGray3,
                                    ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            permissions.requiredGranted -> "Ready to go!"
                            else -> "$requiredGrantedCount of 2 required"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // System Shortcuts Warning
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(tween(800)),
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 20.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Important",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = KeyWaveColors.Warning,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "System volume shortcuts may override KeyWave. If it stops working with the screen off, disable features like:",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Hold Volume for Torch\n• Emergency SOS triggers\n• Camera/Assistant shortcuts",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "These are usually in Settings → Buttons or Gestures.",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textTertiary,
                        )
                    }
                }
            }
            
            // Continue Button
            AnimatedVisibility(
                visible = showCards && permissions.requiredGranted,
                enter = fadeIn(tween(600)),
            ) {
                PillButton(
                    text = "Continue",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    primary = true,
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
        
        // Accessibility Service Prominent Disclosure Dialog
        AccessibilityDisclosureDialog(
            visible = showAccessibilityDisclosure,
            onAgree = {
                showAccessibilityDisclosure = false
                PermissionLauncher.safeStartOrToast(
                    context,
                    PermissionIntents.accessibilitySettings(),
                )
            },
            onDecline = {
                showAccessibilityDisclosure = false
            },
        )
        
        // Notification Listener Prominent Disclosure Dialog
        NotificationDisclosureDialog(
            visible = showNotificationDisclosure,
            onAgree = {
                showNotificationDisclosure = false
                PermissionLauncher.safeStartOrToast(
                    context,
                    PermissionIntents.notificationListenerSettings(),
                )
            },
            onDecline = {
                showNotificationDisclosure = false
            },
        )
    }
}

/**
 * Prominent Disclosure Dialog for Accessibility Service
 * 
 * This dialog fulfills Google Play's requirements for Accessibility Service usage:
 * - Explicitly states what data is being accessed
 * - Explains why the permission is needed
 * - Confirms data is not shared with third parties
 * - Uses "Agree" / "No Thanks" buttons
 */
@Composable
private fun AccessibilityDisclosureDialog(
    visible: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Prevent dismissing by tapping outside */ },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = scaleOut(targetScale = 0.9f, animationSpec = tween(200)),
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        KeyWaveColors.Blue.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "♿",
                                    fontSize = 24.sp,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Accessibility Service Disclosure",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = extended.textPrimary,
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // What data is accessed
                        Text(
                            text = "What KeyWave Accesses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = extended.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KeyWave uses the Accessibility Service API to detect when you press the physical volume buttons on your device. We only monitor volume key press events — no other input, screen content, or personal data is accessed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                            lineHeight = 22.sp,
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Why it's needed
                        Text(
                            text = "Why This Is Needed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = extended.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This permission allows KeyWave to trigger custom media actions (such as skipping to the next track or returning to the previous track) when you hold or double-press the volume buttons, even when your screen is off.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                            lineHeight = 22.sp,
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Data sharing policy
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    KeyWaveColors.Success.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "🔒",
                                    fontSize = 18.sp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Your Privacy",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KeyWaveColors.Success,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "This data is not collected, stored, or shared with third parties. KeyWave has no internet permission and operates entirely offline on your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.textSecondary,
                                        lineHeight = 20.sp,
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // No Thanks button
                            PillButton(
                                text = "No Thanks",
                                onClick = onDecline,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                primary = false,
                            )
                            
                            // Agree button
                            PillButton(
                                text = "Agree",
                                onClick = onAgree,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                primary = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Prominent Disclosure Dialog for Notification Listener Service
 * 
 * This dialog fulfills Google Play's requirements for Notification Listener usage:
 * - Explicitly states what data is being accessed
 * - Explains why the permission is needed
 * - Confirms data is not shared with third parties
 * - Uses "Agree" / "No Thanks" buttons
 */
@Composable
private fun NotificationDisclosureDialog(
    visible: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Prevent dismissing by tapping outside */ },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = scaleOut(targetScale = 0.9f, animationSpec = tween(200)),
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        KeyWaveColors.Blue.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "🔔",
                                    fontSize = 24.sp,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Notification Access Disclosure",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = extended.textPrimary,
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // What data is accessed
                        Text(
                            text = "What KeyWave Accesses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = extended.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KeyWave uses the Notification Listener API to read media session metadata from your notifications. This allows us to identify which music or media app is currently playing. We do not read, store, or access the content of your personal notifications (messages, emails, etc.).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                            lineHeight = 22.sp,
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Why it's needed
                        Text(
                            text = "Why This Is Needed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = extended.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This permission allows KeyWave to detect which media player is active so it can send skip, previous, or play/pause commands to the correct app when you use volume button gestures.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                            lineHeight = 22.sp,
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Data sharing policy
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    KeyWaveColors.Success.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "🔒",
                                    fontSize = 18.sp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Your Privacy",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KeyWaveColors.Success,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "This data is not collected, stored, or shared with third parties. KeyWave has no internet permission and operates entirely offline on your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.textSecondary,
                                        lineHeight = 20.sp,
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // No Thanks button
                            PillButton(
                                text = "No Thanks",
                                onClick = onDecline,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                primary = false,
                            )
                            
                            // Agree button
                            PillButton(
                                text = "Agree",
                                onClick = onAgree,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                primary = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onAction: () -> Unit,
    animationDelay: Int,
    optional: Boolean = false,
) {
    val extended = KeyWaveTheme.extendedColors

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "cardAlpha",
    )

    val cardOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(400),
        label = "cardOffset",
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha
                translationY = cardOffset
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (granted) {
                            KeyWaveColors.Success.copy(alpha = 0.15f)
                        } else {
                            KeyWaveColors.Blue.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (title) {
                        "Accessibility" -> "♿"
                        "Notifications" -> "🔔"
                        "Battery" -> "🔋"
                        else -> "⚙️"
                    },
                    fontSize = 24.sp,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extended.textPrimary,
                    )
                    if (optional) {
                        Text(
                            text = "Optional",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textTertiary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status or Button
            if (granted) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            KeyWaveColors.Success.copy(alpha = 0.15f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = KeyWaveColors.Success,
                    )
                }
            } else {
                PillButton(
                    text = "Enable",
                    onClick = onAction,
                    modifier = Modifier.height(36.dp),
                )
            }
        }
    }
}
