package com.tiborlaszlo.keywave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tiborlaszlo.keywave.ui.theme.KeyWaveColors
import com.tiborlaszlo.keywave.ui.theme.KeyWaveTheme

// ═══════════════════════════════════════════════════════════════
// Glass Card - Frosted glass effect container
// ═══════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(KeyWaveTheme.dimensions.lg),
    content: @Composable () -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors
    val dimensions = KeyWaveTheme.dimensions

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(),
        label = "cardScale",
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (extended.isDark) 0.dp else 2.dp,
                shape = RoundedCornerShape(dimensions.radiusLg),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(dimensions.radiusLg),
        color = extended.glass,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = extended.glassBorder,
        ),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Status Card - Large hero card with optional glow
// ═══════════════════════════════════════════════════════════════

@Composable
fun StatusCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val extended = KeyWaveTheme.extendedColors
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.15f else 0f,
        animationSpec = tween(500),
        label = "glow",
    )

    Box(modifier = modifier) {
        // Glow effect behind card when active
        if (isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KeyWaveColors.Blue.copy(alpha = 0.4f),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(KeyWaveTheme.dimensions.radiusLg),
                    ),
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.displaySmall,
                            color = extended.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                        )
                    }
                    action?.invoke()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// iOS-style Toggle Switch
// ═══════════════════════════════════════════════════════════════

@Composable
fun KeyWaveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val extended = KeyWaveTheme.extendedColors

    val trackColor by animateColorAsState(
        targetValue = if (checked) KeyWaveColors.Success else {
            if (extended.isDark) KeyWaveColors.DarkGray3 else KeyWaveColors.LightGray3
        },
        animationSpec = tween(200),
        label = "trackColor",
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(stiffness = 500f),
        label = "thumbOffset",
    )

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(15.5.dp))
            .background(trackColor)
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(thumbOffset.roundToPx(), 0) }
                .size(27.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Large Power Toggle (for main service)
// ═══════════════════════════════════════════════════════════════

@Composable
fun PowerToggle(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isOn) 1f else 0.95f,
        animationSpec = spring(),
        label = "powerScale",
    )

    val bgColor by animateColorAsState(
        targetValue = if (isOn) KeyWaveColors.Success else KeyWaveColors.DarkGray3,
        animationSpec = tween(300),
        label = "powerBg",
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .scale(scale)
            .shadow(
                elevation = if (isOn) 16.dp else 4.dp,
                shape = CircleShape,
                spotColor = if (isOn) KeyWaveColors.Success.copy(alpha = 0.4f) else Color.Black,
            )
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onToggle(!isOn) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isOn) "ON" else "OFF",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Premium Slider
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyWaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    val extended = KeyWaveTheme.extendedColors

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = KeyWaveColors.Blue,
            activeTrackColor = KeyWaveColors.Blue,
            inactiveTrackColor = if (extended.isDark) {
                KeyWaveColors.DarkGray3
            } else {
                KeyWaveColors.LightGray3
            },
        ),
    )
}

// ═══════════════════════════════════════════════════════════════
// Section Header
// ═══════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val extended = KeyWaveTheme.extendedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = extended.textSecondary,
            letterSpacing = 1.sp,
        )
        action?.invoke()
    }
}

private val androidx.compose.ui.unit.TextUnit.Companion.sp: Nothing
    get() = TODO()

// Fix the import
private val Int.sp: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

private val Float.sp: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)

// ═══════════════════════════════════════════════════════════════
// Setting Row - Standard list item
// ═══════════════════════════════════════════════════════════════

@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val extended = KeyWaveTheme.extendedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 12.dp),
                tint = KeyWaveColors.Blue,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = extended.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary,
                )
            }
        }

        trailing?.invoke()
    }
}

// ═══════════════════════════════════════════════════════════════
// Pill Button
// ═══════════════════════════════════════════════════════════════

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val extended = KeyWaveTheme.extendedColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(),
        label = "buttonScale",
    )

    val backgroundColor = if (primary) {
        KeyWaveColors.Blue
    } else {
        if (extended.isDark) KeyWaveColors.DarkGray3 else KeyWaveColors.LightGray2
    }

    val textColor = if (primary) {
        Color.White
    } else {
        extended.textPrimary
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Status Pill (for permission status)
// ═══════════════════════════════════════════════════════════════

@Composable
fun StatusPill(
    text: String,
    isOk: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOk) {
            KeyWaveColors.Success.copy(alpha = 0.15f)
        } else {
            KeyWaveColors.Error.copy(alpha = 0.15f)
        },
        label = "statusBg",
    )

    val textColor by animateColorAsState(
        targetValue = if (isOk) KeyWaveColors.Success else KeyWaveColors.Error,
        label = "statusText",
    )

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Segmented Control (iOS-style picker)
// ═══════════════════════════════════════════════════════════════

@Composable
fun <T> SegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() },
) {
    val extended = KeyWaveTheme.extendedColors
    val selectedIndex = items.indexOf(selectedItem)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (extended.isDark) KeyWaveColors.DarkGray3 else KeyWaveColors.LightGray2,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (extended.isDark) KeyWaveColors.DarkGray1 else Color.White
                } else Color.Transparent,
                label = "segmentBg",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onItemSelected(item) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = itemLabel(item),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) extended.textPrimary else extended.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Expandable Card
// ═══════════════════════════════════════════════════════════════

@Composable
fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val extended = KeyWaveTheme.extendedColors

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(),
        label = "chevronRotation",
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(KeyWaveTheme.dimensions.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = extended.textPrimary,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary,
                        )
                    }
                }

                trailing?.invoke()

                Spacer(modifier = Modifier.width(8.dp))

                // Chevron
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineLarge,
                    color = extended.textSecondary,
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
                )
            }

            // Content
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = KeyWaveTheme.dimensions.lg,
                            end = KeyWaveTheme.dimensions.lg,
                            bottom = KeyWaveTheme.dimensions.lg,
                        ),
                ) {
                    content()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Action Chip (for action selection)
// ═══════════════════════════════════════════════════════════════

@Composable
fun ActionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = KeyWaveTheme.extendedColors

    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            KeyWaveColors.Blue
        } else {
            if (extended.isDark) KeyWaveColors.DarkGray3 else KeyWaveColors.LightGray2
        },
        label = "chipBg",
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            Color.White
        } else {
            extended.textPrimary
        },
        label = "chipText",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(),
        label = "chipScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}
