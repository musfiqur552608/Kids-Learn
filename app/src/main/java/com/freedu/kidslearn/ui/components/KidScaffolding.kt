package com.freedu.kidslearn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.theme.AnswerCorner
import com.freedu.kidslearn.ui.theme.KidTheme
import com.freedu.kidslearn.ui.theme.MinTouchTarget

/**
 * The coloured app bar every screen sits under.
 *
 * Uses [ModuleType] (not a free colour) so a screen cannot accidentally drift off
 * its subject's identity, and so RTL is derived from the module rather than
 * repeated by hand.
 */
@Composable
fun KidTopBar(
    title: String,
    moduleType: ModuleType?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onReplayAudio: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val accent = moduleType?.let { KidTheme.colors.accentFor(it) }
        ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(accent)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            KidIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                onClick = onBack,
                containerColor = Color.White.copy(alpha = 0.22f),
                contentColor = Color.White,
            )
        } else {
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        if (onReplayAudio != null) {
            KidIconButton(
                icon = Icons.Filled.VolumeUp,
                contentDescription = "Hear it again",
                onClick = onReplayAudio,
                containerColor = Color.White.copy(alpha = 0.22f),
                contentColor = Color.White,
            )
        }
        trailingContent?.invoke()
        Spacer(Modifier.width(8.dp))
    }
}

/**
 * The app-wide page background.
 *
 * A very light vertical gradient rather than a flat colour: it adds depth behind
 * the flat-material cards without any image asset, and a flat background on a
 * large empty area reads as an unfinished screen.
 */
@Composable
fun KidBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

/**
 * One selectable answer in a quiz.
 *
 * Always [MinTouchTarget] tall and uses a large corner radius. The visual state
 * (correct / wrong) is communicated by *colour and a scale bump*, never by
 * removing the option or greying it out - a child who taps the wrong answer must
 * still be able to try it again immediately.
 */
@Composable
fun AnswerTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    state: AnswerTileState = AnswerTileState.IDLE,
    contentDescription: String? = null,
) {
    val kidColors = KidTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val (background, border) = when (state) {
        AnswerTileState.IDLE -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline
        AnswerTileState.SELECTED -> kidColors.starGold.copy(alpha = 0.22f) to kidColors.starGold
        AnswerTileState.CORRECT -> kidColors.successContainer to kidColors.success
        AnswerTileState.GENTLE_NUDGE -> kidColors.tryAgainContainer to kidColors.tryAgain
    }

    Box(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clip(AnswerShape)
            .background(background)
            .border(BORDER_WIDTH_PX.dp, border, AnswerShape)
            .clickableNoRipple(interactionSource, onClick)
            .padding(12.dp)
            .semantics {
                this.contentDescription = contentDescription ?: buildString {
                    append(label)
                    when (state) {
                        AnswerTileState.CORRECT -> append(", correct")
                        AnswerTileState.GENTLE_NUDGE -> append(", let's try again")
                        else -> Unit
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (emoji != null) {
                EmojiTile(
                    emoji = emoji,
                    contentDescription = null,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                )
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Corner radius for an answer tile, shared so tiles and their borders align. */
private val AnswerShape = RoundedCornerShape(AnswerCorner)
private const val BORDER_WIDTH_PX = 3

enum class AnswerTileState { IDLE, SELECTED, CORRECT, GENTLE_NUDGE }

/**
 * A 2x2 grid of answer tiles.
 *
 * A fixed two-column grid rather than a wrapping `FlowRow`: a stable grid means
 * the correct answer never moves between screen sizes, which matters because the
 * child may be memorising position as a reading strategy.
 */
@Composable
fun AnswerGrid(
    options: List<Pair<String, String?>>,
    correctIndex: Int,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowOptions.forEachIndexed { columnIndex, option ->
                    val index = rowIndex * 2 + columnIndex
                    AnswerTile(
                        label = option.first,
                        emoji = option.second,
                        state = when {
                            index == correctIndex && selectedIndex != null -> AnswerTileState.CORRECT
                            index == selectedIndex -> AnswerTileState.GENTLE_NUDGE
                            else -> AnswerTileState.IDLE
                        },
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Odd final row: keep the remaining cell the same width as the
                // others so the grid does not look broken with 3 options.
                if (rowOptions.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
