package com.freedu.kidslearn.ui.maths

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.components.KidIconButton
import com.freedu.kidslearn.ui.components.KidProgressBar
import com.freedu.kidslearn.ui.components.KidTopBar
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.components.StarRow
import com.freedu.kidslearn.ui.components.attentionPulse
import com.freedu.kidslearn.ui.components.clickableNoRipple
import com.freedu.kidslearn.ui.theme.KidTheme
import com.freedu.kidslearn.ui.theme.MinTouchTarget

/**
 * The maths module.
 *
 * Three tabs rather than one long scroll: counting, shapes and plus/minus are
 * genuinely different activities, and tabs let a child return to counting without
 * scrolling past fifteen other cards. Tab order follows increasing difficulty.
 */
@Composable
fun MathsScreen(
    state: MathsUiState,
    onBack: () -> Unit,
    onSelectTab: (MathsTab) -> Unit,
    onSelectNumber: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onTakeQuiz: (MathsTab) -> Unit,
    onPronounce: (com.freedu.kidslearn.domain.model.CountingItem) -> Unit,
    onPronounceShape: (com.freedu.kidslearn.domain.model.ShapeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.maths

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = stringResource(R.string.module_maths),
            moduleType = ModuleType.MATHS,
            onBack = onBack,
        )

        TabRow(
            selectedTabIndex = state.tab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            MathsTab.entries.forEach { tab ->
                Tab(
                    selected = state.tab == tab,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            text = stringResource(tab.labelRes()),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                )
            }
        }

        when (state.tab) {
            MathsTab.COUNTING -> CountingTab(
                state = state,
                accent = accent,
                onSelect = onSelectNumber,
                onClearSelection = onClearSelection,
                onTakeQuiz = { onTakeQuiz(MathsTab.COUNTING) },
                onPronounce = onPronounce,
                modifier = Modifier.weight(1f),
            )

            MathsTab.SHAPES -> ShapesTab(
                shapes = state.shapes.map { it.id to it },
                accent = accent,
                onTakeQuiz = { onTakeQuiz(MathsTab.SHAPES) },
                onPronounceShape = onPronounceShape,
                modifier = Modifier.weight(1f),
            )

            MathsTab.ADDITION -> ArithmeticTab(
                accent = accent,
                onTakeQuiz = { onTakeQuiz(MathsTab.ADDITION) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun MathsTab.labelRes(): Int = when (this) {
    MathsTab.COUNTING -> R.string.maths_counting
    MathsTab.SHAPES -> R.string.maths_shapes
    MathsTab.ADDITION -> R.string.maths_add_subtract
}

/** The 1-20 grid, plus a "count it" view for the selected number. */
@Composable
private fun CountingTab(
    state: MathsUiState,
    accent: Color,
    onSelect: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onTakeQuiz: () -> Unit,
    onPronounce: (com.freedu.kidslearn.domain.model.CountingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selected

    if (selected != null) {
        CountingDetail(
            card = state.countingCards.firstOrNull { it.item.number == selected.number },
            item = selected,
            accent = accent,
            // Back must *clear* the selection: re-selecting the same number would
            // leave the child stuck on this screen with a dead back button.
            onBack = onClearSelection,
            onTakeQuiz = onTakeQuiz,
            onPronounce = { onPronounce(selected) },
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        KidProgressBar(
            progress = state.completionFraction,
            barColor = accent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.countingCards, key = { it.item.id }) { card ->
                NumberCard(
                    card = card,
                    accent = accent,
                    onClick = { onSelect(card.item.number) },
                )
            }
        }
    }
}

/** "How many apples?" - the objects, the numeral, and a way into the quiz. */
@Composable
private fun CountingDetail(
    card: CountingCardUi?,
    item: com.freedu.kidslearn.domain.model.CountingItem,
    accent: Color,
    onBack: () -> Unit,
    onTakeQuiz: () -> Unit,
    onPronounce: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KidIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.word,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // The objects wrap onto multiple lines so a run of 20 never forces a
        // horizontal scroll a five-year-old cannot perform.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 320.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.12f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                item.repeatedVisual.chunked(OBJECTS_PER_ROW).forEach { row ->
                    Text(
                        text = row,
                        fontSize = 40.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )
                }
            }
        }

        Text(
            text = item.number.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = accent,
        )

        if (card?.isCompleted == true) {
            StarRow(stars = card.stars, starSize = 28.dp)
        }

        KidButton(
            text = stringResource(R.string.listen),
            onClick = onPronounce,
            color = accent,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(0.7f),
        )

        Spacer(Modifier.weight(1f))

        KidButton(
            text = stringResource(R.string.maths_tap_the_answer),
            onClick = onTakeQuiz,
            color = accent,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A single number tile. */
@Composable
private fun NumberCard(
    card: CountingCardUi,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val description = "${card.item.number}, ${card.item.word}"

    Column(
        modifier = Modifier
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (card.isCompleted) accent.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickableNoRipple(interactionSource, onClick)
            .semantics { contentDescription = description }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmojiTile(emoji = card.item.visualEmoji, contentDescription = null, fontSize = 30.sp)
        Text(
            text = card.item.number.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Shapes and colours. Tapping a card speaks its name. */
@Composable
private fun ShapesTab(
    shapes: List<Pair<String, com.freedu.kidslearn.domain.model.ShapeItem>>,
    accent: Color,
    onTakeQuiz: () -> Unit,
    onPronounceShape: (com.freedu.kidslearn.domain.model.ShapeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(shapes, key = { it.first }) { (_, item) ->
            val interactionSource = androidx.compose.runtime.remember {
                androidx.compose.foundation.interaction.MutableInteractionSource()
            }
            Column(
                modifier = Modifier
                    .heightIn(min = 104.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickableNoRipple(interactionSource, onClick = { onPronounceShape(item) })
                    .semantics { contentDescription = item.shapeName }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmojiTile(emoji = item.visual, contentDescription = null, fontSize = 40.sp)
                Text(
                    text = item.shapeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            KidButton(
                text = stringResource(R.string.maths_tap_the_answer),
                onClick = onTakeQuiz,
                color = accent,
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}

/**
 * Plus and minus.
 *
 * There is deliberately no teaching content here beyond the mascot prompt: single
 * digit arithmetic is a *game* for this age, and the actual instruction happens in
 * the quiz itself. A screen of static examples would be the one part of the app
 * that requires a child who cannot yet read to understand a visual convention.
 */
@Composable
private fun ArithmeticTab(
    accent: Color,
    onTakeQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Mascot(
            mood = MascotMood.HAPPY,
            size = 130.dp,
            modifier = Modifier.attentionPulse(),
        )
        Text(
            text = stringResource(R.string.maths_add_subtract_subtitle),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "3 + 2", style = MaterialTheme.typography.displaySmall, color = accent)
            Text(text = "5 − 1", style = MaterialTheme.typography.displaySmall, color = accent)
        }
        Spacer(Modifier.height(8.dp))
        KidButton(
            text = stringResource(R.string.maths_tap_the_answer),
            onClick = onTakeQuiz,
            color = accent,
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget),
        )
    }
}

private const val OBJECTS_PER_ROW = 6
