package com.freedu.kidslearn.ui.games

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.ui.alphabet.subtitleRes
import com.freedu.kidslearn.ui.alphabet.titleRes
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidTopBar
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.components.clickableNoRipple
import com.freedu.kidslearn.ui.theme.ModuleTileHeight
import com.freedu.kidslearn.ui.theme.KidTheme

/** The Games hub: three games, each with the child's personal best. */
@Composable
fun GamesHubScreen(
    state: GamesHubUiState,
    onBack: () -> Unit,
    onOpenGame: (GameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.games

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.games_title),
            moduleType = null,
            onBack = onBack,
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(mood = MascotMood.HAPPY, size = 88.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.games_subtitle),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            items(state.games, key = { it.gameType.name }) { card ->
                GameCard(
                    card = card,
                    accent = accent,
                    onClick = { onOpenGame(card.gameType) },
                )
            }
        }
    }
}

@Composable
private fun GameCard(
    card: GameCardUi,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val icon = iconFor(card.gameType)
    val description = stringResource(card.gameType.titleRes()) + ", " +
        pluralStringResource(R.plurals.game_best, card.bestScore, card.bestScore)

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
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.width(52.dp).height(52.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(card.gameType.titleRes()),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = stringResource(card.gameType.subtitleRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        if (card.bestScore > 0) {
            Column(horizontalAlignment = Alignment.End) {
                EmojiTile(emoji = "🏆", contentDescription = null, fontSize = 24.sp)
                Text(
                    text = card.bestScore.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
        }
    }
}

private fun iconFor(gameType: GameType): ImageVector = when (gameType) {
    GameType.MEMORY_MATCH -> Icons.Filled.GridView
    GameType.FIND_THE_CORRECT_ONE -> Icons.Filled.EmojiEvents
    GameType.TIMED_QUIZ -> Icons.Filled.Timer
}
