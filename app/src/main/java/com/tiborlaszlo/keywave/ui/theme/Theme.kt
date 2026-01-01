package com.tiborlaszlo.keywave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
// Color Palette - Apple-inspired
// ═══════════════════════════════════════════════════════════════

object KeyWaveColors {
    // Primary - iOS Blue
    val Blue = Color(0xFF0A84FF)
    val BlueDark = Color(0xFF0071E3)
    val BlueLight = Color(0xFF5AC8FA)

    // Semantic
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFFD60A)
    val Error = Color(0xFFFF453A)

    // Neutral - Dark Mode
    val SpaceBlack = Color(0xFF0D0D0F)
    val DarkGray1 = Color(0xFF1C1C1E)
    val DarkGray2 = Color(0xFF2C2C2E)
    val DarkGray3 = Color(0xFF3A3A3C)
    val DarkGray4 = Color(0xFF48484A)
    val DarkGray5 = Color(0xFF636366)
    val DarkGray6 = Color(0xFF8E8E93)

    // Neutral - Light Mode
    val White = Color(0xFFFFFFFF)
    val LightGray1 = Color(0xFFF2F2F7)
    val LightGray2 = Color(0xFFE5E5EA)
    val LightGray3 = Color(0xFFD1D1D6)
    val LightGray4 = Color(0xFFC7C7CC)
    val LightGray5 = Color(0xFFAEAEB2)

    // Glass backgrounds
    val GlassDark = Color(0xFF1C1C1E).copy(alpha = 0.72f)
    val GlassLight = Color(0xFFFFFFFF).copy(alpha = 0.72f)
    val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val GlassBorderLight = Color(0xFF000000).copy(alpha = 0.06f)

    // Gradients (represented as list of colors)
    val GradientPrimary = listOf(Color(0xFF0A84FF), Color(0xFF5856D6))
    val GradientSuccess = listOf(Color(0xFF30D158), Color(0xFF34C759))
    val GradientBackground = listOf(Color(0xFF1C1C1E), Color(0xFF0D0D0F))
}

// ═══════════════════════════════════════════════════════════════
// Material Color Schemes
// ═══════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary = KeyWaveColors.Blue,
    onPrimary = KeyWaveColors.White,
    primaryContainer = KeyWaveColors.BlueDark,
    onPrimaryContainer = KeyWaveColors.White,
    secondary = KeyWaveColors.DarkGray3,
    onSecondary = KeyWaveColors.White,
    secondaryContainer = KeyWaveColors.DarkGray2,
    onSecondaryContainer = KeyWaveColors.White,
    tertiary = KeyWaveColors.BlueLight,
    onTertiary = KeyWaveColors.SpaceBlack,
    background = KeyWaveColors.SpaceBlack,
    onBackground = KeyWaveColors.White,
    surface = KeyWaveColors.DarkGray1,
    onSurface = KeyWaveColors.White,
    surfaceVariant = KeyWaveColors.DarkGray2,
    onSurfaceVariant = KeyWaveColors.DarkGray6,
    outline = KeyWaveColors.DarkGray4,
    outlineVariant = KeyWaveColors.DarkGray3,
    error = KeyWaveColors.Error,
    onError = KeyWaveColors.White,
)

private val LightColorScheme = lightColorScheme(
    primary = KeyWaveColors.Blue,
    onPrimary = KeyWaveColors.White,
    primaryContainer = KeyWaveColors.BlueLight,
    onPrimaryContainer = KeyWaveColors.SpaceBlack,
    secondary = KeyWaveColors.LightGray2,
    onSecondary = KeyWaveColors.SpaceBlack,
    secondaryContainer = KeyWaveColors.LightGray1,
    onSecondaryContainer = KeyWaveColors.SpaceBlack,
    tertiary = KeyWaveColors.BlueDark,
    onTertiary = KeyWaveColors.White,
    background = KeyWaveColors.LightGray1,
    onBackground = KeyWaveColors.SpaceBlack,
    surface = KeyWaveColors.White,
    onSurface = KeyWaveColors.SpaceBlack,
    surfaceVariant = KeyWaveColors.LightGray2,
    onSurfaceVariant = KeyWaveColors.DarkGray5,
    outline = KeyWaveColors.LightGray3,
    outlineVariant = KeyWaveColors.LightGray4,
    error = KeyWaveColors.Error,
    onError = KeyWaveColors.White,
)

// ═══════════════════════════════════════════════════════════════
// Typography - SF Pro inspired (using system sans-serif)
// ═══════════════════════════════════════════════════════════════

private val KeyWaveTypography = Typography(
    // Large Title
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp,
    ),
    // Title 1
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp,
    ),
    // Title 2
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp,
    ),
    // Title 3
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp,
    ),
    // Headline
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp,
    ),
    // Subheadline
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp,
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp,
    ),
    // Callout
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp,
    ),
    // Footnote
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp,
    ),
    // Caption 1
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    // Caption 2
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.06.sp,
    ),
    // Tiny
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.sp,
    ),
)

// ═══════════════════════════════════════════════════════════════
// Dimensions - Consistent spacing
// ═══════════════════════════════════════════════════════════════

data class KeyWaveDimensions(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,

    // Corner radii
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 16.dp,
    val radiusXl: Dp = 20.dp,
    val radiusFull: Dp = 100.dp,

    // Card sizing
    val cardElevation: Dp = 0.dp,
    val iconSizeSm: Dp = 20.dp,
    val iconSizeMd: Dp = 24.dp,
    val iconSizeLg: Dp = 28.dp,
    val iconSizeXl: Dp = 44.dp,

    // Touch targets
    val minTouchTarget: Dp = 44.dp,
)

val LocalKeyWaveDimensions = staticCompositionLocalOf { KeyWaveDimensions() }

// ═══════════════════════════════════════════════════════════════
// Extended Colors - For glassmorphism and gradients
// ═══════════════════════════════════════════════════════════════

data class KeyWaveExtendedColors(
    val isDark: Boolean,
    val glass: Color,
    val glassBorder: Color,
    val success: Color,
    val warning: Color,
    val gradientPrimary: List<Color>,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
)

val LocalKeyWaveExtendedColors = staticCompositionLocalOf {
    KeyWaveExtendedColors(
        isDark = true,
        glass = KeyWaveColors.GlassDark,
        glassBorder = KeyWaveColors.GlassBorderDark,
        success = KeyWaveColors.Success,
        warning = KeyWaveColors.Warning,
        gradientPrimary = KeyWaveColors.GradientPrimary,
        textPrimary = KeyWaveColors.White,
        textSecondary = KeyWaveColors.DarkGray6,
        textTertiary = KeyWaveColors.DarkGray5,
    )
}

// ═══════════════════════════════════════════════════════════════
// Theme Composable
// ═══════════════════════════════════════════════════════════════

@Composable
fun KeyWaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extendedColors = if (darkTheme) {
        KeyWaveExtendedColors(
            isDark = true,
            glass = KeyWaveColors.GlassDark,
            glassBorder = KeyWaveColors.GlassBorderDark,
            success = KeyWaveColors.Success,
            warning = KeyWaveColors.Warning,
            gradientPrimary = KeyWaveColors.GradientPrimary,
            textPrimary = KeyWaveColors.White,
            textSecondary = KeyWaveColors.DarkGray6,
            textTertiary = KeyWaveColors.DarkGray5,
        )
    } else {
        KeyWaveExtendedColors(
            isDark = false,
            glass = KeyWaveColors.GlassLight,
            glassBorder = KeyWaveColors.GlassBorderLight,
            success = KeyWaveColors.Success,
            warning = KeyWaveColors.Warning,
            gradientPrimary = KeyWaveColors.GradientPrimary,
            textPrimary = KeyWaveColors.SpaceBlack,
            textSecondary = KeyWaveColors.DarkGray5,
            textTertiary = KeyWaveColors.DarkGray6,
        )
    }

    CompositionLocalProvider(
        LocalKeyWaveExtendedColors provides extendedColors,
        LocalKeyWaveDimensions provides KeyWaveDimensions(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KeyWaveTypography,
            content = content,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Convenience accessors
// ═══════════════════════════════════════════════════════════════

object KeyWaveTheme {
    val extendedColors: KeyWaveExtendedColors
        @Composable
        get() = LocalKeyWaveExtendedColors.current

    val dimensions: KeyWaveDimensions
        @Composable
        get() = LocalKeyWaveDimensions.current
}
