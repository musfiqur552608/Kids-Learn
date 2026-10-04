package com.freedu.kidslearn.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.components.AnswerTile
import com.freedu.kidslearn.ui.components.AnswerTileState
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.components.KidTopBar
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.components.MascotSpeech
import com.freedu.kidslearn.ui.components.StatPill
import com.freedu.kidslearn.ui.components.attentionPulse
import com.freedu.kidslearn.ui.components.clickableNoRipple
import com.freedu.kidslearn.ui.theme.MinTouchTarget
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * Memory match board: a 3-column grid of face-down cards.
 *
 * ## Card faces
 * A card shows either a `?` or the letter pair. There is no "wrong" animation and
 * no red state - a mismatched pair simply turns back over after a beat, which is
 * the whole feedback loop for this age group.
 */
@Composable
fun MemoryMatchScreen(
    state: MemoryUiState,
    onBack: () -> Unit,
    onCardTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.games

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.game_memory_match),
            moduleType = null,
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatPill(
                icon = Icons.Filled.TouchApp,
                value = "${state.matchedPairs}/${state.totalPairs}",
                label = stringResource(R.string.game_pairs),
                tint = accent,
            )
            Text(
                text = pluralStringResource(R.plurals.game_taps_to_match, state.taps, state.taps),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.cards, key = { it.id }) { card ->
                MemoryCardView(
                    card = card,
                    accent = accent,
                    onClick = { onCardTapped(card.id) },
                )
            }
        }
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCardUi,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    // Face-down cards are the module colour, face-up cards are white with the
    // module accent border. `animateColorAsState` makes the flip feel physical.
    val background by animateColorAsState(
        targetValue = when {
            card.isFaceUp -> MaterialTheme.colorScheme.surface
            card.isMatched -> KidTheme.colors.success.copy(alpha = 0.25f)
            else -> accent
        },
        label = "card-face",
    )
    val contentDescription = when {
        card.isFaceUp -> card.glyph
        card.isMatched -> stringResource(R.string.quiz_answer_correct)
        else -> stringResource(R.string.game_memory_match)
    }

    Box(
        modifier = Modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickableNoRipple(interactionSource, onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (card.isFaceUp) {
            Text(
                text = card.glyph,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = if (card.isMatched) "✓" else "?",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
        }
    }
}

/**
 * "Find the correct one": one target letter, four options.
 *
 * The target pulses gently. It is the only moving thing on the screen, so a child
 * who has not yet read the instruction still knows where to look.
 */
@Composable
fun FindCorrectScreen(
    state: FindCorrectUiState,
    onBack: () -> Unit,
    onOptionTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.games

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.game_find_correct),
            moduleType = null,
            onBack = onBack,
        )

        MascotSpeech(
            message = stringResource(R.string.game_find_correct_subtitle),
            mood = if (state.mistakes > 0) MascotMood.ENCOURAGE else MascotMood.HAPPY,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.14f))
                .then(Modifier.attentionPulse()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.targetGlyph,
                style = MaterialTheme.typography.displayLarge,
                color = accent,
            )
        }

        Text(
            text = pluralStringResource(R.plurals.game_rounds, state.round + 1, state.round + 1, state.totalRounds),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.options.forEachIndexed { index, option ->
                AnswerTile(
                    label = option,
                    state = AnswerTileState.IDLE,
                    onClick = { onOptionTapped(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

/**
 * "Odd one out": three tiles match, one does not.
 *
 * The round counter and the pulsing mascot line carry the instruction, so the
 * grid itself stays quiet: four large glyphs with no competing decoration.
 */
@Composable
fun OddOneOutScreen(
    state: OddOneOutUiState,
    onBack: () -> Unit,
    onOptionTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.games

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.game_odd_one_out),
            moduleType = null,
            onBack = onBack,
        )

        MascotSpeech(
            message = stringResource(R.string.game_odd_one_out_subtitle),
            mood = if (state.mistakes > 0) MascotMood.ENCOURAGE else MascotMood.HAPPY,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Text(
            text = pluralStringResource(R.plurals.game_rounds, state.round + 1, state.round + 1, state.totalRounds),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.options.forEachIndexed { index, option ->
                AnswerTile(
                    label = option,
                    state = AnswerTileState.IDLE,
                    onClick = { onOptionTapped(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

/**
 * The timed quiz.
 *
 * The countdown is a plain number, and it only changes colour in the last three
 * seconds. No flashing, no animation - a stressed child needs a stable screen, and
 * a ticking clock that pulses is exactly the wrong feedback for someone who is
 * already anxious about the clock.
 */
@Composable
fun TimedQuizScreen(
    state: TimedQuizUiState,
    onBack: () -> Unit,
    onAnswerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.games
    val question = state.question
    val urgent = state.secondsLeft <= 3 && state.isRunning

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.game_timed_quiz),
            moduleType = ModuleType.MATHS,
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatPill(
                icon = Icons.Filled.Timer,
                value = state.secondsLeft.toString(),
                label = stringResource(R.string.game_seconds_short),
                tint = if (urgent) KidTheme.colors.tryAgain else accent,
            )
            Text(
                text = pluralStringResource(R.plurals.game_score, state.score, state.score),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
            )
        }

        if (question == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.12f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = question.promptLabel ?: question.promptVisual,
                style = if (question.promptLabel != null) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = accent,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            question.options.forEachIndexed { index, option ->
                AnswerTile(
                    label = option.label,
                    emoji = option.visual,
                    onClick = { onAnswerSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}
