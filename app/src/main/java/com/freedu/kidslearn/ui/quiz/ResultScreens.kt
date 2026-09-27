package com.freedu.kidslearn.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
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
import com.freedu.kidslearn.domain.model.GameResult
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizResult
import com.freedu.kidslearn.ui.alphabet.labelRes
import com.freedu.kidslearn.ui.components.BigStat
import com.freedu.kidslearn.ui.components.CelebrationOverlay
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.components.StarRow
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * Shown after a quiz or a game.
 *
 * ## Always celebrates
 * Even a score of 1/5 gets the full mascot, the confetti and a star row. The copy
 * changes (a perfect run is called out as "Perfect!") but the *experience* does
 * not: a child who got one wrong must not be told they did badly, because the next
 * thing they see is the screen deciding whether the game was fun.
 *
 * The star row honestly shows the stars actually earned - withholding them would
 * be dishonest and the parent would notice - but it is framed as a starting
 * point, not a grade.
 */
@Composable
fun QuizResultScreen(
    result: QuizResult,
    showCelebration: Boolean,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    onDismissCelebration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.accentFor(result.moduleType)
    val perfect = result.correctAnswers == result.totalQuestions && result.totalQuestions > 0

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(
                mood = if (perfect) MascotMood.PROUD else MascotMood.CHEER,
                size = 140.dp,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(
                    if (perfect) R.string.result_title_perfect else R.string.result_title,
                ),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            StarRow(stars = result.starsEarned, starSize = 48.dp)

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BigStat(
                    value = pluralStringResource(
                        R.plurals.quiz_correct_count,
                        result.correctAnswers,
                        result.correctAnswers,
                        result.totalQuestions,
                    ),
                    label = stringResource(result.moduleType.labelRes()),
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                BigStat(
                    value = "+${result.coinsEarned}",
                    label = stringResource(R.string.cd_coin),
                    color = KidTheme.colors.coinGold,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(28.dp))

            KidButton(
                text = stringResource(R.string.result_play_again),
                onClick = onPlayAgain,
                color = accent,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            KidButton(
                text = stringResource(R.string.result_go_home),
                onClick = onGoHome,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Confetti over the whole screen, dismissed by the child. A one-shot
        // animation the child can dismiss beats an automatic one they cannot
        // escape, which is how you get an app they swipe past.
        CelebrationOverlay(
            visible = showCelebration,
            message = pluralStringResource(
                R.plurals.result_stars_earned,
                result.starsEarned,
                result.starsEarned,
            ),
            onFinished = onDismissCelebration,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
        )
    }
}

/** The same layout, for a finished mini-game. */
@Composable
fun GameResultScreen(
    result: GameResult,
    showCelebration: Boolean,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    onDismissCelebration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.games

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(mood = MascotMood.CHEER, size = 140.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.game_you_matched),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            StarRow(stars = result.starsEarned, starSize = 48.dp)
            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BigStat(
                    value = pluralStringResource(R.plurals.game_score, result.score, result.score),
                    label = stringResource(R.string.progress_games),
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                BigStat(
                    value = "+${result.coinsEarned}",
                    label = stringResource(R.string.cd_coin),
                    color = KidTheme.colors.coinGold,
                    modifier = Modifier.weight(1f),
                )
            }

            if (result.isNewPersonalBest) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.result_new_badge),
                    style = MaterialTheme.typography.titleLarge,
                    color = KidTheme.colors.starGold,
                )
            }

            Spacer(Modifier.height(28.dp))
            KidButton(
                text = stringResource(R.string.result_play_again),
                onClick = onPlayAgain,
                color = accent,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            KidButton(
                text = stringResource(R.string.result_go_home),
                onClick = onGoHome,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        CelebrationOverlay(
            visible = showCelebration,
            message = stringResource(R.string.yay),
            onFinished = onDismissCelebration,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
        )
    }
}
