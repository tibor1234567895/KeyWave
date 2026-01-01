package com.tiborlaszlo.keywave.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiborlaszlo.keywave.ui.components.GlassCard
import com.tiborlaszlo.keywave.ui.components.PillButton
import com.tiborlaszlo.keywave.ui.components.StatusPill
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
                            PermissionLauncher.safeStartOrToast(
                                context,
                                PermissionIntents.accessibilitySettings(),
                            )
                        },
                        animationDelay = 0,
                    )

                    PermissionCard(
                        title = "Notifications",
                        description = "Find the active media player",
                        granted = permissions.notificationListenerEnabled,
                        onAction = {
                            PermissionLauncher.safeStartOrToast(
                                context,
                                PermissionIntents.notificationListenerSettings(),
                            )
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
