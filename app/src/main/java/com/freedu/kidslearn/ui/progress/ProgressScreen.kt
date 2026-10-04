package com.freedu.kidslearn.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.alphabet.emoji as badgeEmoji
import com.freedu.kidslearn.ui.alphabet.labelRes
import com.freedu.kidslearn.ui.alphabet.titleRes
import com.freedu.kidslearn.ui.components.BadgeChip
import com.freedu.kidslearn.ui.components.BigStat
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidIconButton
import com.freedu.kidslearn.ui.components.KidTopBar
import com.freedu.kidslearn.ui.components.LabelledProgress
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * The progress dashboard.
 *
 * ## What a child sees
 * Stars, coins and a streak - all things they earned themselves. Locked badges are
 * visible but greyed, so there is a visible next goal. Progress percentages are
 * numeric because this screen is also opened by parents, who want the precise
 * figure.
 */
@Composable
fun ProgressScreen(
    state: DashboardSnapshot,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = state.stats
    val unlocked = state.badges.map { it.badgeKey }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.progress_title),
            moduleType = null,
            onBack = onBack,
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(mood = MascotMood.PROUD, size = 80.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.progress_overall),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.progress_percent, state.overallPercent, state.overallPercent),
                            style = MaterialTheme.typography.headlineMedium,
                            color = KidTheme.colors.progress,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigStat(
                        value = stats.totalStars.toString(),
                        label = stringResource(R.string.progress_total_stars),
                        color = KidTheme.colors.starGold,
                        modifier = Modifier.weight(1f),
                    )
                    BigStat(
                        value = stats.totalCoins.toString(),
                        label = stringResource(R.string.progress_total_coins),
                        color = KidTheme.colors.coinGold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigStat(
                        value = stats.currentStreak.toString(),
                        label = stringResource(R.string.progress_streak),
                        color = KidTheme.colors.tryAgain,
                        modifier = Modifier.weight(1f),
                    )
                    BigStat(
                        value = stats.longestStreak.toString(),
                        label = stringResource(R.string.progress_best_streak),
                        color = KidTheme.colors.arabic,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.home_modules),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            items(state.modules, key = { it.moduleType.name }) { module ->
                LabelledProgress(
                    title = androidx.compose.ui.res.stringResource(module.moduleType.labelRes()),
                    completed = module.completedItems,
                    total = module.totalItems,
                    barColor = KidTheme.colors.accentFor(module.moduleType),
                    percentText = pluralStringResource(
                        R.plurals.progress_percent,
                        module.completionPercent,
                        module.completionPercent,
                    ),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.progress_badges) + " · " +
                        pluralStringResource(
                            R.plurals.progress_badges_earned,
                            state.badges.size,
                            state.badges.size,
                            BadgeKey.entries.size,
                        ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            item {
                // Locked badges stay visible: a goal the child cannot see is not a
                // goal. `LazyRow` rather than a grid because eleven chips in a
                // wrapping grid is a lot of vertical space on a small screen.
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BadgeKey.entries, key = { it.name }) { key ->
                        BadgeChip(
                            emoji = key.badgeEmoji(),
                            title = androidx.compose.ui.res.stringResource(key.titleRes()),
                            unlocked = key in unlocked,
                            modifier = Modifier.width(96.dp),
                        )
                    }
                }
            }

            if (state.recentScores.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.progress_recent),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                // The key includes the index: two identical games on the same day
                // (two perfect memory runs both score 6) would otherwise share a
                // key, and duplicate LazyColumn keys misbehave.
                itemsIndexed(
                    state.recentScores,
                    key = { index, score -> "$index-${score.gameType}-${score.playedAt}-${score.score}" },
                ) { _, score ->
                    // Localised game name: the raw enum (MEMORY_MATCH) is
                    // developer vocabulary, never TalkBack copy.
                    val scoreDescription =
                        "${stringResource(score.gameType.titleRes())}: ${score.score}"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp)
                            .semantics {
                                contentDescription = scoreDescription
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(score.gameType.titleRes()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = score.score.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = KidTheme.colors.games,
                        )
                    }
                }
            }
        }
    }
}
