package com.freedu.kidslearn.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freedu.kidslearn.ui.theme.KidTheme
import com.freedu.kidslearn.ui.theme.MinTouchTarget
import com.freedu.kidslearn.ui.theme.PrimaryButtonHeight

/**
 * The app's primary call-to-action.
 *
 * ## Why the press feedback is a *scale* and not a colour change
 * A ripple requires the child to look at the button they pressed and see a
 * change. A scale-down is visible in peripheral vision and is confirmed at the
 * point of contact, which is where a child aged 3-5 is actually looking. The
 * ripple is disabled deliberately - the scale reads as more responsive and is
 * less noisy on a low-end device (no separate draw pass for the ripple).
 */
@Composable
fun KidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // Animated rather than snapped: the spring-back is the playful part, and a
    // hard 0.94 jump reads as a glitch on a big colourful button.
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) PRESSED_SCALE else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "kidbutton-press",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            // 72dp tall, comfortably above the 64dp minimum, so the button is easy
            // to hit while walking around with the device.
            .defaultMinSize(minHeight = PrimaryButtonHeight, minWidth = MinTouchTarget)
            .scale(scale)
            .semantics { contentDescription = text },
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A circular, icon-only button - used for back navigation and audio replay.
 *
 * Always 64dp square so it meets the minimum target, regardless of the icon it
 * holds.
 */
@Composable
fun KidIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "kidiconbutton-press",
    )

    Box(
        modifier = modifier
            .size(MinTouchTarget)
            .scale(scale)
            .background(containerColor, RoundedCornerShape(20.dp))
            .clickableNoRipple(interactionSource, onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(30.dp),
        )
    }
}

/**
 * A row of 1-3 stars.
 *
 * Read out to TalkBack as a single phrase ("2 out of 3 stars") rather than three
 * separate nodes, which would otherwise be announced as "star, star, empty star".
 */
@Composable
fun StarRow(
    stars: Int,
    modifier: Modifier = Modifier,
    maxStars: Int = 3,
    starSize: androidx.compose.ui.unit.Dp = 32.dp,
    showEmpty: Boolean = true,
) {
    val description = "$stars out of $maxStars stars"
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(maxStars) { index ->
            val earned = index < stars
            // Each star is cleared from the merge so it is not announced separately.
            Icon(
                imageVector = if (earned) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (earned) KidTheme.colors.starGold else KidTheme.colors.starEmpty,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

/** Decorative emoji. Cleared from the a11y tree unless the caller supplies a label. */
@Composable
fun EmojiTile(
    emoji: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.displaySmall.fontSize,
) {
    Text(
        text = emoji,
        fontSize = fontSize,
        textAlign = TextAlign.Center,
        modifier = if (contentDescription == null) {
            modifier.clearAndSetSemantics { }
        } else {
            modifier.semantics { this.contentDescription = contentDescription }
        },
    )
}

/** A labelled pill used for counts (coins, streaks, items completed). */
@Composable
fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .background(KidTheme.colors.starGold.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$value $label" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Section heading above a group of cards. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A slow, continuous "breathing" scale used to draw a child toward the one
 * control that matters on a screen (the mascot's call to action).
 *
 * Deliberately low amplitude and slow: a fast bounce reads as an error or
 * notification, and anything above [SLOW_PULSE_MS] is visually stressful for this
 * age group.
 */
@Composable
fun Modifier.attentionPulse(
    enabled: Boolean = true,
    minScale: Float = 1f,
    maxScale: Float = 1.06f,
): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "attention-pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SLOW_PULSE_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "attention-scale",
    )
    return this.scale(scale)
}

private const val PRESSED_SCALE = 0.94f
private const val SLOW_PULSE_MS = 1400
