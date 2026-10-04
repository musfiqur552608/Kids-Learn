package com.freedu.kidslearn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Playful motion for the kid-facing tiles.
 *
 * Two pieces, both deliberately bouncy rather than snappy: a fast bounce reads
 * as alive and responsive to a 3-5 year old, while an instant snap or a slow
 * fade reads as broken or boring. Amplitudes stay small (≤ 4% scale, 1/5-height
 * slide) so nothing ever looks like an error shake or a notification.
 */

/**
 * Squishes a tile while pressed, springing back on release.
 *
 * Pairs with [clickableNoRipple]: the ripple is off app-wide (it muddies the
 * saturated tile colours), so this scale *is* the press feedback. Applied on
 * top of the background so the whole tile - colour included - squishes.
 */
@Composable
fun Modifier.pressBounce(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "press-bounce",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fades + rises content in, staggered by list position.
 *
 * Used for the Home module tiles and the Games cards so the screen assembles
 * itself instead of blinking in whole. The delay caps at six steps so a long
 * list never leaves its tail invisible, and each step is 60ms - perceptible as
 * a cascade, too fast to feel like loading.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_STEP_MS * index.coerceAtMost(MAX_STAGGER_INDEX).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(ENTRANCE_FADE_MS)) +
            slideInVertically(animationSpec = tween(ENTRANCE_SLIDE_MS)) { it / 5 },
        modifier = modifier,
    ) {
        content()
    }
}

private const val ENTRANCE_STEP_MS = 60L
private const val MAX_STAGGER_INDEX = 6
private const val ENTRANCE_FADE_MS = 280
private const val ENTRANCE_SLIDE_MS = 320
