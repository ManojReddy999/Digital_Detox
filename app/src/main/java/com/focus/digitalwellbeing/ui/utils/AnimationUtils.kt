package com.focus.digitalwellbeing.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Animation durations
const val ANIMATION_DURATION_SHORT = 200
const val ANIMATION_DURATION_MEDIUM = 300
const val ANIMATION_DURATION_LONG = 400

// Spring animation specs
val DefaultSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

// Fade animation specs
val FadeInSpec = fadeIn(animationSpec = tween(ANIMATION_DURATION_MEDIUM))
val FadeOutSpec = fadeOut(animationSpec = tween(ANIMATION_DURATION_MEDIUM))

// Slide animation specs
val SlideInLeftSpec = slideInHorizontally(
    animationSpec = tween(ANIMATION_DURATION_MEDIUM),
    initialOffsetX = { -it }
)
val SlideOutRightSpec = slideOutHorizontally(
    animationSpec = tween(ANIMATION_DURATION_MEDIUM),
    targetOffsetX = { it }
)

val SlideInRightSpec = slideInHorizontally(
    animationSpec = tween(ANIMATION_DURATION_MEDIUM),
    initialOffsetX = { it }
)
val SlideOutLeftSpec = slideOutHorizontally(
    animationSpec = tween(ANIMATION_DURATION_MEDIUM),
    targetOffsetX = { -it }
)

// Scale animation modifier with bounce effect
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = DefaultSpring,
        label = "bounce_scale"
    )
    
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

// Shimmer effect for loading states
fun Modifier.shimmer(show: Boolean = true) = composed {
    if (!show) return@composed this
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    this.graphicsLayer {
        alpha = 0.5f + 0.3f * kotlin.math.sin(translateAnim / 200f)
    }
}

// Animated visibility with slide and fade
@Composable
fun AnimatedVisibilitySlide(
    visible: Boolean,
    slideDirection: SlideDirection = SlideDirection.Up,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = when (slideDirection) {
            SlideDirection.Up -> slideInVertically(
                animationSpec = tween(ANIMATION_DURATION_MEDIUM),
                initialOffsetY = { it }
            ) + FadeInSpec
            SlideDirection.Down -> slideInVertically(
                animationSpec = tween(ANIMATION_DURATION_MEDIUM),
                initialOffsetY = { -it }
            ) + FadeInSpec
            SlideDirection.Left -> SlideInLeftSpec + FadeInSpec
            SlideDirection.Right -> SlideInRightSpec + FadeInSpec
        },
        exit = when (slideDirection) {
            SlideDirection.Up -> slideOutVertically(
                animationSpec = tween(ANIMATION_DURATION_MEDIUM),
                targetOffsetY = { -it }
            ) + FadeOutSpec
            SlideDirection.Down -> slideOutVertically(
                animationSpec = tween(ANIMATION_DURATION_MEDIUM),
                targetOffsetY = { it }
            ) + FadeOutSpec
            SlideDirection.Left -> SlideOutLeftSpec + FadeOutSpec
            SlideDirection.Right -> SlideOutRightSpec + FadeOutSpec
        },
        content = content
    )
}

// Crossfade transition for tab changes
@Composable
fun AnimatedCrossfade(
    targetState: Int,
    content: @Composable (Int) -> Unit
) {
    Crossfade(
        targetState = targetState,
        animationSpec = tween(ANIMATION_DURATION_MEDIUM),
        label = "crossfade"
    ) { state ->
        content(state)
    }
}

enum class SlideDirection {
    Up, Down, Left, Right
}

// Staggered list item animation
fun staggeredFadeInSpec(index: Int, delayPerItem: Int = 50) = fadeIn(
    animationSpec = tween(
        durationMillis = ANIMATION_DURATION_MEDIUM,
        delayMillis = index * delayPerItem
    )
) + slideInVertically(
    animationSpec = tween(
        durationMillis = ANIMATION_DURATION_MEDIUM,
        delayMillis = index * delayPerItem
    ),
    initialOffsetY = { it / 2 }
)

