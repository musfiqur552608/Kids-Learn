package com.freedu.kidslearn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * A `clickable` with no ripple.
 *
 * ## Why the ripple is off for the big kid-facing tiles
 * Material's ripple is drawn in a `surfaceColor`-blended colour that is tuned for
 * neutral greys. Over a saturated module tile (an orange Games tile, a green
 * Arabic tile) it produces a muddy, low-contrast smudge that reads as a rendering
 * bug rather than feedback. These tiles get their feedback from a scale animation
 * in [KidButton] / [KidIconButton] instead, which is both clearer and cheaper to
 * draw.
 *
 * The explicit [indication] parameter is still passed (as `null`) rather than left
 * to the default, so the intent is visible at the call site.
 */
fun Modifier.clickableNoRipple(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier = composed {
    clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}
