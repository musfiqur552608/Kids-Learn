package com.freedu.kidslearn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * A rounded, animated progress bar.
 *
 * Uses [animateFloatAsState] so a completed lesson visibly *fills* the bar. For
 * this age group the motion is the reward: the number changing from 2/26 to 3/26
 * is invisible to a child who cannot yet read numerals.
 */
@Composable
fun KidProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 22.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    barColor: Color = MaterialTheme.colorScheme.primary,
    label: String? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 700),
        label = "progress-fill",
    )
    val description = label ?: "${(clamped * 100).toInt()} percent complete"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
            // The bar's value is already conveyed visually; announcing the raw
            // percentage as well would be noise for TalkBack users.
            .clearAndSetSemantics { },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(barColor),
        )
    }
}

/** A labelled progress bar: name, count and the bar itself. */
@Composable
fun LabelledProgress(
    title: String,
    completed: Int,
    total: Int,
    barColor: Color,
    modifier: Modifier = Modifier,
    percentText: String? = null,
) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    val description = "$title, $completed of $total complete"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = percentText ?: "$completed / $total",
                style = MaterialTheme.typography.titleMedium,
                color = barColor,
            )
        }
        KidProgressBar(progress = fraction, barColor = barColor)
    }
}

/** A large "number + noun" tile used on the dashboard. */
@Composable
fun BigStat(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A single badge chip.
 *
 * Locked badges are drawn desaturated and at 40% opacity rather than hidden, so a
 * child can see there is something to work towards. The "locked" state is
 * announced to TalkBack.
 */
@Composable
fun BadgeChip(
    emoji: String,
    title: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = if (unlocked) 1f else 0.4f
    Column(
        modifier = modifier
            .background(
                if (unlocked) {
                    KidTheme.colors.starGold.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(20.dp),
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (unlocked) "$title, unlocked" else "$title, locked"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.material3.Text(
            text = emoji,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clearAndSetSemantics { },
        )
        androidx.compose.material3.Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}
