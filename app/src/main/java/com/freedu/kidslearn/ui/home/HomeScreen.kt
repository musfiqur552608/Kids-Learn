package com.freedu.kidslearn.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.freedu.kidslearn.ui.alphabet.labelRes
import com.freedu.kidslearn.ui.alphabet.tileEmoji
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.components.KidIconButton
import com.freedu.kidslearn.ui.components.KidProgressBar
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.theme.ModuleTileHeight
import com.freedu.kidslearn.ui.components.StatPill
import com.freedu.kidslearn.ui.components.attentionPulse
import com.freedu.kidslearn.ui.components.clickableNoRipple
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * The Home screen: four subject tiles, a Games tile, and a Progress tile.
 *
 * ## Navigation by colour, not by reading
 * A pre-reader cannot use the labels, so each subject owns a colour that appears
 * on its tile, its top bar and its cards. The tile pairs that colour with a large
 * emoji, giving three redundant cues (colour, image, position) so a child can
 * reach "Arabic" without being able to read the word.
 *
 * ## Why the parent controls are small and bottom-right
 * The Progress and Grown-ups tiles are deliberately the least prominent things on
 * the screen. A child should be able to reach every *learning* destination without
 * passing a settings door, and the parent gate on the zone itself is the
 * protection that matters.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenModule: (ModuleType) -> Unit,
    onOpenGames: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenParentZone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = state.greeting

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(mood = MascotMood.HAPPY, size = 84.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (greeting != null) {
                            stringResource(R.string.home_greeting_named, greeting)
                        } else {
                            stringResource(R.string.home_greeting)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.currentStreak > 0) {
            item {
                StreakBanner(
                    streak = state.currentStreak,
                    celebrate = state.showStreakPrompt,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(
                    icon = Icons.Filled.Star,
                    value = state.totalStars.toString(),
                    label = stringResource(R.string.progress_total_stars),
                    tint = KidTheme.colors.starGold,
                )
                StatPill(
                    icon = Icons.Filled.MonetizationOn,
                    value = state.totalCoins.toString(),
                    label = stringResource(R.string.progress_total_coins),
                    tint = KidTheme.colors.coinGold,
                )
            }
        }

        if (state.lastModule != null) {
            item {
                KidButton(
                    text = stringResource(
                        R.string.home_continue_with,
                        stringResource(state.lastModule!!.labelRes()),
                    ),
                    onClick = { onOpenModule(state.lastModule!!) },
                    color = KidTheme.colors.accentFor(state.lastModule!!),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
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

        items(ModuleType.entries, key = { it.name }) { module ->
            val tile = state.tiles.firstOrNull { it.moduleType == module }
            ModuleTile(
                moduleType = module,
                emoji = module.tileEmoji(),
                completed = tile?.completed ?: 0,
                total = tile?.total ?: 0,
                onClick = { onOpenModule(module) },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SquareTile(
                    emoji = "🎮",
                    titleRes = R.string.module_games,
                    accent = KidTheme.colors.games,
                    onClick = onOpenGames,
                    modifier = Modifier.weight(1f),
                )
                SquareTile(
                    emoji = "📊",
                    titleRes = R.string.module_progress,
                    accent = KidTheme.colors.progress,
                    onClick = onOpenProgress,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                KidIconButton(
                    icon = Icons.Filled.LocalFireDepartment,
                    contentDescription = stringResource(R.string.module_parent_zone),
                    onClick = onOpenParentZone,
                    containerColor = KidTheme.colors.parentZoneContainer,
                    contentColor = KidTheme.colors.parentZone,
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** One large subject tile. */
@Composable
private fun ModuleTile(
    moduleType: ModuleType,
    emoji: String,
    completed: Int,
    total: Int,
    onClick: () -> Unit,
) {
    val accent = KidTheme.colors.accentFor(moduleType)
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    // Resolved through `pluralStringResource` rather than
    // `LocalContext.current.getString`: reading a resource directly from a Context
    // bypasses Compose's resource-change tracking, so the label would go stale
    // after a configuration change.
    //
    // Argument order matters here: `pluralStringResource(id, quantity, vararg
    // formatArgs)` - the quantity selects the plural form and is *not* a format
    // argument. The format string is "%1$s: %2$d of %3$d complete", so the three
    // arguments are (label, completed, total).
    val description = pluralStringResource(
        R.plurals.cd_module_progress,
        completed,
        stringResource(moduleType.labelRes()),
        completed,
        total,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ModuleTileHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(accent)
            .clickableNoRipple(interactionSource, onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiTile(emoji = emoji, contentDescription = null, fontSize = 44.sp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = androidx.compose.ui.res.stringResource(moduleType.labelRes()),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            KidProgressBar(
                progress = if (total == 0) 0f else completed.toFloat() / total,
                barColor = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                height = 12.dp,
            )
        }
    }
}

/** A smaller square tile for Games / Progress. */
@Composable
private fun SquareTile(
    emoji: String,
    titleRes: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    Column(
        modifier = modifier
            .height(ModuleTileHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(accent.copy(alpha = 0.16f))
            .clickableNoRipple(interactionSource, onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmojiTile(emoji = emoji, contentDescription = null, fontSize = 38.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/** The daily streak, with an extra flourish on a milestone. */
@Composable
private fun StreakBanner(streak: Int, celebrate: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(KidTheme.colors.tryAgainContainer)
            .then(if (celebrate) Modifier.attentionPulse() else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiTile(emoji = "🔥", contentDescription = null, fontSize = 30.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.progress_streak) + ": $streak",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
