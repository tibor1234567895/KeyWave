package com.tiborlaszlo.keywave.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

// ═══════════════════════════════════════════════════════════════
// Animation Specs - iOS-like timing curves
// ═══════════════════════════════════════════════════════════════

object KeyWaveAnimations {
    // Standard iOS spring animation
    val springDefault: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    // Snappy spring for quick feedback
    val springSnappy: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    // Gentle spring for smooth transitions
    val springGentle: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    // iOS-standard 0.3s ease
    val tweenDefault = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
    )

    // Quick feedback
    val tweenFast = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )

    // Slow, smooth transitions
    val tweenSlow = tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing,
    )

    // Animation durations in ms
    const val DURATION_FAST = 150
    const val DURATION_DEFAULT = 300
    const val DURATION_SLOW = 500
    const val DURATION_STAGGER = 50
}

// ═══════════════════════════════════════════════════════════════
// Scale-on-press modifier (iOS button feedback)
// ═══════════════════════════════════════════════════════════════

enum class PressState { Idle, Pressed }

fun Modifier.scaleOnPress(
    pressedScale: Float = 0.96f,
): Modifier = composed {
    var pressState by remember { mutableStateOf(PressState.Idle) }

    val scale by animateFloatAsState(
        targetValue = if (pressState == PressState.Pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "scale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressState = PressState.Pressed
                    waitForUpOrCancellation()
                    pressState = PressState.Idle
                }
            }
        }
}

// ═══════════════════════════════════════════════════════════════
// Bounce animation for success feedback
// ═══════════════════════════════════════════════════════════════

@Composable
fun animateBounce(trigger: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (trigger) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bounce",
    )
    return scale
}

// ═══════════════════════════════════════════════════════════════
// Fade animation helper
// ═══════════════════════════════════════════════════════════════

@Composable
fun animateFade(visible: Boolean): Float {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = KeyWaveAnimations.DURATION_DEFAULT,
            easing = FastOutSlowInEasing,
        ),
        label = "fade",
    )
    return alpha
}

// ═══════════════════════════════════════════════════════════════
// Staggered animation delay calculator
// ═══════════════════════════════════════════════════════════════

fun staggeredDelay(index: Int, baseDelay: Int = KeyWaveAnimations.DURATION_STAGGER): Int {
    return index * baseDelay
}
