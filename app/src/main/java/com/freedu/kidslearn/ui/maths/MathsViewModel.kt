package com.freedu.kidslearn.ui.maths

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.CountingItem
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.ShapeItem
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.usecase.RememberLastModuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The four maths sub-modules a child can enter. */
enum class MathsTab {
    COUNTING,
    SHAPES,
    ADDITION,
    ;

    val route: String get() = name.lowercase()
}

/** A counting card on the maths grid. */
data class CountingCardUi(
    val item: CountingItem,
    val isCompleted: Boolean,
    val stars: Int,
)

data class MathsUiState(
    val tab: MathsTab = MathsTab.COUNTING,
    val countingCards: List<CountingCardUi> = emptyList(),
    val shapes: List<ShapeItem> = emptyList(),
    val completedItems: Int = 0,
    val totalItems: Int = 0,
    val totalStars: Int = 0,
    val selected: CountingItem? = null,
    val isLoading: Boolean = true,
) {
    val completionFraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems
}

/**
 * The maths module: counting 1-20, shapes & colours, and plus/minus.
 *
 * ## Why maths does not use the alphabet ViewModel
 * Its lessons are numbers and shapes rather than letters, so a "letter grid" state
 * would be misleading. The shape is similar, so the progress/selection
 * conventions are kept identical - a child moving from English to Maths sees the
 * same stars, the same cards and the same audio.
 */
@HiltViewModel
class MathsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    private val rememberLastModule: RememberLastModuleUseCase,
    private val feedbackPlayer: FeedbackPlayer,
) : ViewModel() {

    private val tab = MutableStateFlow(
        savedStateHandle.get<String>(KEY_TAB)
            ?.let { name -> MathsTab.entries.firstOrNull { it.name == name } }
            ?: MathsTab.COUNTING,
    )
    private val selectedNumber = MutableStateFlow<Int?>(
        savedStateHandle.get<String>(KEY_SELECTED_NUMBER)?.toIntOrNull(),
    )

    init {
        viewModelScope.launch { rememberLastModule(ModuleType.MATHS) }
    }

    val uiState: StateFlow<MathsUiState> = combine(
        progressRepository.observeLessonProgress(ModuleType.MATHS),
        tab,
        selectedNumber,
    ) { progress, currentTab, number ->
        val items = contentRepository.countingItems()
        val byId = progress.associateBy { it.itemId }
        MathsUiState(
            tab = currentTab,
            countingCards = items.map { item ->
                val row = byId[item.id]
                CountingCardUi(
                    item = item,
                    isCompleted = row?.isCompleted == true,
                    stars = row?.starsEarned ?: 0,
                )
            },
            shapes = contentRepository.shapeItems(),
            completedItems = progress.count { it.isCompleted },
            // Single source of truth with the dashboard: counting + shapes.
            totalItems = contentRepository.totalItems(ModuleType.MATHS),
            totalStars = progress.sumOf { it.starsEarned },
            selected = number?.let { n -> items.firstOrNull { it.number == n } },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = MathsUiState(),
    )

    fun selectTab(next: MathsTab) {
        tab.value = next
        selectedNumber.value = null
    }

    fun selectNumber(number: Int) {
        selectedNumber.value = number
    }

    fun clearSelection() {
        selectedNumber.value = null
    }

    /** Counts a number aloud, one object at a time is too slow, so the word is used. */
    fun pronounceCount(item: CountingItem) {
        feedbackPlayer.onTap()
        feedbackPlayer.pronouncePrompt(item.word, FeedbackPlayer.Locales.ENGLISH)
    }

    /** Reads a shape/colour name aloud. Shapes had no sound path at all. */
    fun pronounceShape(item: ShapeItem) {
        feedbackPlayer.onTap()
        val text = if (item.colourName.isNotEmpty() && item.colourName != item.shapeName) {
            "${item.colourName} ${item.shapeName}"
        } else {
            item.shapeName
        }
        feedbackPlayer.pronouncePrompt(text, FeedbackPlayer.Locales.ENGLISH)
    }

    private companion object {
        const val KEY_TAB = "mathsTab"
        const val KEY_SELECTED_NUMBER = "selectedNumber"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
