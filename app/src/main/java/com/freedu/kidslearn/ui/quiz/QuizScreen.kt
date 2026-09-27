package com.freedu.kidslearn.ui.quiz

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizKind
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.ui.alphabet.labelRes
import com.freedu.kidslearn.ui.components.AnswerGrid
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.components.KidIconButton
import com.freedu.kidslearn.ui.components.KidProgressBar
import com.freedu.kidslearn.ui.components.KidTopBar
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.components.MascotSpeech
import com.freedu.kidslearn.ui.components.StatPill
import com.freedu.kidslearn.ui.components.attentionPulse
import com.freedu.kidslearn.ui.theme.KidTheme
import com.freedu.kidslearn.ui.theme.MinTouchTarget

/**
 * The quiz screen, shared by every subject.
 *
 * ## Why one quiz screen
 * The four modules ask the same five question *shapes* (see [QuizKind]); only the
 * content differs. A single screen parameterised by [QuizUiState] means the answer
 * grid, the mascot prompt, the progress bar and the auto-advance rule exist once.
 * A per-module quiz screen would be four files that differ only in their data.
 *
 * ## Positive-only feedback
 * A wrong tap nudges the mascot into [MascotMood.ENCOURAGE] and says "let us try
 * that one again". The tile turns amber, never red, and stays tappable. There is
 * no buzzer, no cross and no lock-out anywhere in this file.
 */
@Composable
fun QuizScreen(
    state: QuizUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onReplayPrompt: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.accentFor(state.moduleType)
    val question = state.currentQuestion

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(state.moduleType.labelRes()),
            moduleType = state.moduleType,
            onBack = onBack,
            onReplayAudio = question?.let { { onReplayPrompt() } },
        )

        if (question == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            return@Column
        }

        KidProgressBar(
            progress = state.progress,
            barColor = accent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.quiz_question_of,
                    state.currentIndex + 1,
                    state.currentIndex + 1,
                    state.questions.size,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatPill(
                icon = Icons.Filled.Star,
                value = state.correctCount.toString(),
                label = "/ ${state.questions.size}",
                tint = KidTheme.colors.starGold,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MascotSpeech(
                message = promptTextFor(state, question),
                mood = when {
                    state.mistakesOnCurrent > 0 -> MascotMood.ENCOURAGE
                    state.hasAnsweredCurrent -> MascotMood.CHEER
                    else -> MascotMood.HAPPY
                },
            )

            QuestionPrompt(
                question = question,
                accent = accent,
                moduleType = state.moduleType,
            )

            AnswerGrid(
                options = question.options.map { it.label to it.visual },
                correctIndex = question.correctIndex,
                selectedIndex = state.selectedIndex,
                onSelect = onAnswer,
            )

            // The child controls the pace. Auto-advance also happens, but a
            // pre-reader who wants to look at the answer longer needs a way to
            // stay put, and a big obvious button is the only reliable one.
            if (state.hasAnsweredCurrent) {
                KidButton(
                    text = stringResource(
                        if (state.isLastQuestion) R.string.quiz_finish else R.string.next,
                    ),
                    onClick = onNext,
                    color = KidTheme.colors.success,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Chooses the mascot line from the question type, in the app's language. */
@Composable
private fun promptTextFor(state: QuizUiState, question: QuizQuestion): String = when {
    state.mistakesOnCurrent > 0 -> stringResource(R.string.quiz_answer_try_again)
    state.hasAnsweredCurrent -> stringResource(R.string.quiz_answer_correct)
    question.kind == QuizKind.COUNT_OBJECTS -> stringResource(R.string.quiz_how_many)
    question.kind == QuizKind.PICTURE_TO_LETTER -> stringResource(R.string.quiz_tap_the_letter)
    else -> stringResource(R.string.quiz_tap_the_picture)
}

/** The big thing being asked about: an emoji, a letter, or a sum. */
@Composable
private fun QuestionPrompt(
    question: QuizQuestion,
    accent: Color,
    moduleType: ModuleType,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .background(accent.copy(alpha = 0.12f), MaterialTheme.shapes.large)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (question.promptVisual.isNotEmpty()) {
                if (question.kind == QuizKind.COUNT_OBJECTS) {
                    // The objects wrap so a long run (20 stars) stays on screen
                    // instead of forcing a horizontal scroll.
                    question.promptVisual.chunked(OBJECTS_PER_ROW).forEach { row ->
                        Text(
                            text = row,
                            fontSize = 34.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 40.sp,
                        )
                    }
                } else {
                    EmojiTile(
                        emoji = question.promptVisual,
                        contentDescription = null,
                        fontSize = 84.sp,
                        modifier = Modifier.attentionPulse(enabled = question.promptVisual.isNotEmpty()),
                    )
                }
            }
            if (!question.promptLabel.isNullOrEmpty()) {
                Text(
                    text = question.promptLabel,
                    style = MaterialTheme.typography.displaySmall,
                    color = accent,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Objects per row when counting. Seven fits a phone at 34sp without truncating. */
private const val OBJECTS_PER_ROW = 7
