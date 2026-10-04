package com.freedu.kidslearn.ui.alphabet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.LetterCategory
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.domain.usecase.RememberLastModuleUseCase
import com.freedu.kidslearn.ui.components.TraceQuality
import com.freedu.kidslearn.ui.components.TraceResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A letter plus the child's own progress on it. */
data class LetterCardUi(
    val item: LetterItem,
    val isCompleted: Boolean,
    val stars: Int,
)

/**
 * A group of letters under a heading.
 *
 * The heading is held as a [LetterCategory] rather than a string so the composable
 * can resolve it through `stringResource` and the whole section localises with the
 * rest of the app.
 */
data class LetterSectionUi(
    val category: LetterCategory,
    val letters: List<LetterCardUi>,
)

/** Which sub-screen of a module the user is looking at. */
enum class AlphabetStage {
    /** A preview of the module, with a "start" button. */
    OVERVIEW,

    /** The grid of letters. */
    LETTER_LIST,

    /** One letter, with audio and tracing. */
    DETAIL,
}

/** State for the whole alphabet module - one ViewModel serves all three stages. */
data class AlphabetUiState(
    val moduleType: ModuleType = ModuleType.ENGLISH,
    val stage: AlphabetStage = AlphabetStage.OVERVIEW,
    val sections: List<LetterSectionUi> = emptyList(),
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val totalStars: Int = 0,
    val selected: LetterItem? = null,
    val settings: AppSettings = AppSettings.DEFAULT,
    val isLoading: Boolean = true,
) {
    val completionFraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems
}

/**
 * Drives the English, Bangla and Arabic modules.
 *
 * ## Why all three alphabets share one implementation
 * The three modules are *the same screen with different content and different
 * fonts*. Separate implementations would mean three near-identical files, and
 * every future change to quiz entry, progress display or tracing would have to be
 * written three times. The module is a constructor parameter, so the shared
 * behaviour lives here once.
 *
 * The concrete `@HiltViewModel` subclasses - [EnglishViewModel],
 * [BanglaViewModel], [ArabicViewModel] - exist only to pin the module type, since
 * Hilt cannot inject a bare `ModuleType`. Each is a single constructor call.
 *
 * ## State shape
 * One immutable [AlphabetUiState] rather than several `StateFlow`s. The screen
 * renders a single `when` over [AlphabetStage], so one state object means the UI
 * can never show a half-updated combination (new letters alongside a stale item
 * count).
 */
abstract class AlphabetViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val rememberLastModule: RememberLastModuleUseCase,
    private val feedbackPlayer: FeedbackPlayer,
    /** The subject this instance teaches. Fixed by the concrete subclass. */
    val moduleType: ModuleType,
) : ViewModel() {

    private val stage = MutableStateFlow(AlphabetStage.OVERVIEW)

    /**
     * The selected letter is seeded from [SavedStateHandle] so it survives process
     * death: a child who is killed by the OS while reading "A for Apple" comes
     * back to the same letter, not to the list.
     */
    private val selectedItemId = MutableStateFlow<String?>(savedStateHandle[KEY_SELECTED_ITEM])

    init {
        // Home offers "keep learning" using this, so it must be set as soon as the
        // module is opened - not when a letter is selected.
        viewModelScope.launch { rememberLastModule(moduleType) }
    }

    val uiState: StateFlow<AlphabetUiState> = combine(
        progressRepository.observeLessonProgress(moduleType),
        settingsRepository.settings,
        stage,
        selectedItemId,
    ) { progress, settings, currentStage, selectedId ->
        val letters = contentRepository.letters(moduleType)
        val byId = progress.associateBy { it.itemId }

        // `groupBy` preserves first-encounter order, so the catalog's teaching
        // order (vowels before consonants) is preserved in the UI.
        val sections = letters
            .groupBy { it.category }
            .map { (category, items) ->
                LetterSectionUi(
                    category = category,
                    letters = items.map { item ->
                        val row = byId[item.id]
                        LetterCardUi(
                            item = item,
                            isCompleted = row?.isCompleted == true,
                            stars = row?.starsEarned ?: 0,
                        )
                    },
                )
            }

        AlphabetUiState(
            moduleType = moduleType,
            stage = currentStage,
            sections = sections,
            totalItems = letters.size,
            completedItems = progress.count { it.isCompleted },
            totalStars = progress.sumOf { it.starsEarned },
            selected = selectedId?.let { id -> letters.firstOrNull { it.id == id } },
            settings = settings,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AlphabetUiState(moduleType = moduleType),
    )

    // -------------------------------------------------------------- navigation

    fun showOverview() = stage.update { AlphabetStage.OVERVIEW }

    fun showLetterList() = stage.update { AlphabetStage.LETTER_LIST }

    fun selectLetter(item: LetterItem) {
        selectedItemId.value = item.id
        savedStateHandle[KEY_SELECTED_ITEM] = item.id
        stage.update { AlphabetStage.DETAIL }
    }

    fun clearSelection() {
        selectedItemId.value = null
        savedStateHandle[KEY_SELECTED_ITEM] = null
        stage.update { AlphabetStage.LETTER_LIST }
    }

    // ------------------------------------------------------------------ audio

    /** Reads the letter and its example word aloud. */
    fun pronounce(item: LetterItem) {
        feedbackPlayer.onTap()
        feedbackPlayer.pronounceLesson(moduleType, item.letter, item.exampleWord)
    }

    /**
     * Celebrates a finished trace with sound only.
     *
     * Tracing is practice, not assessment: it awards no stars and records no
     * progress, but a successful trace with zero feedback reads as a dead
     * interaction, so a good trace earns the star sound.
     */
    fun onTraceFinished(result: TraceResult) {
        if (result.quality == TraceQuality.SUCCESS) {
            feedbackPlayer.onStarEarned()
        }
    }

    // ------------------------------------------------------------------- quiz

    suspend fun currentSettings(): AppSettings = settingsRepository.settings.first()

    protected companion object {
        const val KEY_SELECTED_ITEM = "selectedItemId"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
