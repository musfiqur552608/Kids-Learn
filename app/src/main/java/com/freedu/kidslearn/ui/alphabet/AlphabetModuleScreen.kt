package com.freedu.kidslearn.ui.alphabet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.components.EmojiTile
import com.freedu.kidslearn.ui.components.KidProgressBar
import com.freedu.kidslearn.ui.components.KidTopBar
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.components.MascotSpeech
import com.freedu.kidslearn.ui.components.StarRow
import com.freedu.kidslearn.ui.components.attentionPulse
import com.freedu.kidslearn.ui.components.clickableNoRipple
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.theme.KidTheme
import com.freedu.kidslearn.ui.theme.ArabicTextStyle
import com.freedu.kidslearn.ui.theme.BanglaTextStyle
import com.freedu.kidslearn.ui.theme.KidTheme as Theme

/**
 * The single screen that renders an alphabet module.
 *
 * All three stages (overview, letter grid, letter detail) live in one composable
 * because they share the same state object, the same top bar and the same accent
 * colour, and because splitting them would mean three files that differ only in
 * which block of the state they read.
 *
 * @param onTakeQuiz receives the letter the quiz should cover, or null for a
 *   mixed quiz across the whole module
 */
@Composable
fun AlphabetModuleScreen(
    state: AlphabetUiState,
    onBack: () -> Unit,
    onShowOverview: () -> Unit,
    onShowLetterList: () -> Unit,
    onSelectLetter: (LetterItem) -> Unit,
    onClearSelection: () -> Unit,
    onPronounce: (LetterItem) -> Unit,
    onTakeQuiz: (LetterItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.accentFor(state.moduleType)
    // Every script gets its own font. Falling back to the default (Roboto) would
    // render Bangla and Arabic as blank boxes on most devices.
    val letterStyle = when (state.moduleType) {
        ModuleType.ENGLISH -> MaterialTheme.typography.displayLarge
        ModuleType.BANGLA -> BanglaTextStyle
        ModuleType.ARABIC -> ArabicTextStyle
        ModuleType.MATHS -> MaterialTheme.typography.displayLarge
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        KidTopBar(
            title = when (state.stage) {
                AlphabetStage.OVERVIEW -> androidx.compose.ui.res.stringResource(state.moduleType.labelRes())
                AlphabetStage.LETTER_LIST -> androidx.compose.ui.res.stringResource(com.freedu.kidslearn.R.string.all_letters)
                AlphabetStage.DETAIL -> state.selected?.letter
                    ?: androidx.compose.ui.res.stringResource(state.moduleType.labelRes())
            },
            moduleType = state.moduleType,
            onBack = when (state.stage) {
                AlphabetStage.OVERVIEW -> onBack
                AlphabetStage.LETTER_LIST -> onShowOverview
                AlphabetStage.DETAIL -> onClearSelection
            },
            onReplayAudio = state.selected?.let { item -> { onPronounce(item) } },
        )

        when (state.stage) {
            AlphabetStage.OVERVIEW -> AlphabetOverview(
                state = state,
                accent = accent,
                onStart = onShowLetterList,
                onTakeQuiz = { onTakeQuiz(null) },
                modifier = Modifier.weight(1f),
            )

            AlphabetStage.LETTER_LIST -> LetterGrid(
                state = state,
                letterStyle = letterStyle,
                onSelect = onSelectLetter,
                modifier = Modifier.weight(1f),
            )

            AlphabetStage.DETAIL -> state.selected?.let { item ->
                LetterDetail(
                    item = item,
                    moduleType = state.moduleType,
                    letterStyle = letterStyle,
                    onBack = onClearSelection,
                    onPronounce = { onPronounce(item) },
                    onTakeQuiz = { onTakeQuiz(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Module landing page: what is this subject, how far have we got, start. */
@Composable
private fun AlphabetOverview(
    state: AlphabetUiState,
    accent: Color,
    onStart: () -> Unit,
    onTakeQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Mascot(
                mood = MascotMood.HAPPY,
                size = 120.dp,
                modifier = Modifier.attentionPulse(),
            )

            Text(
                text = androidx.compose.ui.res.stringResource(state.moduleType.labelRes()),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            KidProgressBar(
                progress = state.completionFraction,
                barColor = accent,
                label = androidx.compose.ui.res.pluralStringResource(
                    com.freedu.kidslearn.R.plurals.letters_count,
                    state.completedItems,
                    state.completedItems,
                    state.totalItems,
                ),
            )

            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    com.freedu.kidslearn.R.plurals.letters_count,
                    state.completedItems,
                    state.completedItems,
                    state.totalItems,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KidButton(
                    text = androidx.compose.ui.res.stringResource(com.freedu.kidslearn.R.string.all_letters),
                    onClick = onStart,
                    color = accent,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                )
                KidButton(
                    text = androidx.compose.ui.res.stringResource(com.freedu.kidslearn.R.string.take_quiz),
                    onClick = onTakeQuiz,
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The grid of letter cards, grouped into vowel / consonant sections. */
@Composable
private fun LetterGrid(
    state: AlphabetUiState,
    letterStyle: androidx.compose.ui.text.TextStyle,
    onSelect: (LetterItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.accentFor(state.moduleType)
    // Two columns on a phone. A child can hold two cards in mind at once, and a
    // single column would push 26 letters into 26 screens of scrolling.
    val columns = GridCells.Fixed(2)

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.sections.forEach { section ->
            item(key = "header-${section.category.name}", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                val title = categoryLabelRes(section.category)
                if (title != null) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = accent,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
            }
            items(
                items = section.letters,
                key = { it.item.id },
            ) { card ->
                LetterCard(
                    card = card,
                    letterStyle = letterStyle,
                    accent = accent,
                    onClick = { onSelect(card.item) },
                )
            }
        }
    }
}

/**
 * One letter card.
 *
 * A completed card gets a filled accent border and a star row, so progress is
 * visible at a glance in a 2-column grid - the child does not need to read the
 * stars number, only see that the card is "done".
 */
@Composable
private fun LetterCard(
    card: LetterCardUi,
    letterStyle: androidx.compose.ui.text.TextStyle,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val description = stringResource(
        com.freedu.kidslearn.R.string.letter_card_description,
        card.item.letter,
        card.item.exampleWord,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (card.isCompleted) accent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickableNoRipple(interactionSource, onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmojiTile(
            emoji = card.item.visual,
            contentDescription = null,
            fontSize = 34.sp,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = card.item.letter,
                style = letterStyle.copy(fontSize = letterStyle.fontSize * 0.42f),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Visible,
            )
            if (card.item.secondary.isNotEmpty()) {
                Text(
                    text = card.item.secondary,
                    style = letterStyle.copy(fontSize = letterStyle.fontSize * 0.30f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (card.isCompleted) {
            Spacer(Modifier.height(4.dp))
            StarRow(stars = card.stars, starSize = 16.dp, showEmpty = false)
        }
    }
}

/**
 * One letter in detail: the big glyph, its sound, its word, tracing, and the way
 * into the quiz.
 *
 * The layout is a single scrolling column rather than a pager because a child
 * scrolls confidently but does not reliably swipe horizontally - and a horizontal
 * swipe that is *not* understood as a page change is the single most common source
 * of accidental navigation in this age group.
 */
@Composable
private fun LetterDetail(
    item: LetterItem,
    moduleType: ModuleType,
    letterStyle: androidx.compose.ui.text.TextStyle,
    onBack: () -> Unit,
    onPronounce: () -> Unit,
    onTakeQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.accentFor(moduleType)
    val guide = TraceGuideSupport.forLetter(item.letter)

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            MascotSpeech(
                message = item.transliteration,
                mood = MascotMood.HAPPY,
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = item.letter,
                        style = letterStyle,
                        color = accent,
                    )
                    if (item.secondary.isNotEmpty()) {
                        Text(
                            text = item.secondary,
                            style = letterStyle.copy(fontSize = letterStyle.fontSize * 0.45f),
                            color = accent.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                EmojiTile(emoji = item.visual, contentDescription = null, fontSize = 72.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.exampleWord,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.exampleMeaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                KidButton(
                    text = androidx.compose.ui.res.stringResource(com.freedu.kidslearn.R.string.listen),
                    onClick = onPronounce,
                    color = accent,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(0.7f),
                )
            }
        }

        if (guide != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.freedu.kidslearn.R.string.trace_instruction),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    com.freedu.kidslearn.ui.components.LetterTracingCanvas(
                        guide = guide,
                        onTraceFinished = { },
                        accentColor = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                }
            }
        }

        item {
            KidButton(
                text = androidx.compose.ui.res.stringResource(com.freedu.kidslearn.R.string.take_quiz),
                onClick = onTakeQuiz,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Maps a display letter to a traceable guide, or null when there is none. */
private object TraceGuideSupport {
    fun forLetter(letter: String): com.freedu.kidslearn.ui.components.TraceGuide? =
        com.freedu.kidslearn.ui.components.TraceGuide.entries
            .firstOrNull { it.label.equals(letter.trim(), ignoreCase = true) }
}
