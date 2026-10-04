package com.freedu.kidslearn.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.domain.model.QuizResult
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.usecase.RecordQuizResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Where the quiz currently is. */
enum class QuizStage {
    /** Playing. */
    ACTIVE,

    /** Every question answered; a save is in flight. */
    FINISHING,

    /** Result has been written. */
    COMPLETE,
}

/**
 * Immutable state of a quiz in progress.
 *
 * @param mistakesOnCurrent question how many times the child has got this one
 *   wrong so far. Used only for copy, never to block progress.
 * @param selectedIndex which option the child tapped. It also guards the
 *   auto-advance: tapping the right answer four times must not advance
 *   the question four times.
 */
data class QuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val stage: QuizStage = QuizStage.ACTIVE,
    val selectedIndex: Int? = null,
    val mistakesOnCurrent: Int = 0,
    val correctCount: Int = 0,
    val moduleType: ModuleType = ModuleType.ENGLISH,
    val result: QuizResult? = null,
    /**
     * Whether the confetti overlay is up.
     *
     * Held here rather than in the composable so "play again" and "dismiss the
     * celebration" are both state transitions the ViewModel owns - the overlay
     * cannot end up stuck on screen because a recomposition happened to recreate a
     * `remember`.
     */
    val showCelebration: Boolean = false,
    val isLoading: Boolean = true,
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val isLastQuestion: Boolean get() = currentIndex >= questions.lastIndex

    /** True once the current question has been answered correctly. */
    val hasAnsweredCurrent: Boolean get() = selectedIndex != null

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size
}

/**
 * The quiz engine, shared by every subject and by the "find the correct one" game.
 *
 * ## Positive-only feedback, enforced structurally
 * A wrong answer does not advance, does not lock the option and does not produce a
 * negative sound or a red cross - it sets
 * [QuizUiState.mistakesOnCurrent] and leaves the question on screen. The child
 * simply taps something else. Only the mascot copy and the soft sound change, and
 * only the counter used for the parent's statistics records that it happened.
 *
 * This is not a UI decision that could drift: there is no code path that advances
 * the index on an incorrect answer.
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val recordQuizResult: RecordQuizResultUseCase,
    private val feedbackPlayer: FeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val moduleType: ModuleType = savedStateHandle.get<String>(ARG_MODULE)
        ?.let { name -> ModuleType.entries.firstOrNull { it.name == name } }
        ?: ModuleType.ENGLISH

    private val itemIds: List<String> =
        com.freedu.kidslearn.ui.navigation.Route.Quiz.parseItemIds(
            savedStateHandle.get<String>(ARG_ITEM_IDS),
        )

    private val questionCount: Int = savedStateHandle.get<String>(ARG_QUESTION_COUNT)
        ?.toIntOrNull()
        ?.coerceIn(1, MAX_QUESTIONS)
        ?: DEFAULT_QUESTION_COUNT

    private val seed: Long = savedStateHandle.get<String>(ARG_SEED)?.toLongOrNull() ?: 0L

    /**
     * Subjects the child has answered correctly at least once this run.
     *
     * Passing exactly these - rather than the route's item filter - to the
     * recorder is what makes "X of 26 learned" move after a mixed quiz covering
     * several letters, and what stops a quiz where everything was missed from
     * marking anything learned. A question answered correctly after retries
     * still counts: eventual success is success.
     */
    private val learnedItemIds = mutableSetOf<String>()

    init {
        val questions = contentRepository.buildQuiz(
            moduleType = moduleType,
            itemIds = itemIds,
            count = questionCount,
            seed = seed,
        )
        _uiState.update {
            it.copy(
                questions = questions,
                moduleType = moduleType,
                isLoading = false,
            )
        }
        questions.firstOrNull()?.let { speakPrompt(it) }
    }

    /**
     * Handles a tap on an answer.
     *
     * The `hasAnsweredCurrent` guard is what stops a double-tap - very common with
     * this age group - from advancing two questions at once.
     */
    fun onAnswerSelected(index: Int) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.stage != QuizStage.ACTIVE || state.hasAnsweredCurrent) return

        feedbackPlayer.onTap()

        if (index == question.correctIndex) {
            feedbackPlayer.onCorrect()
            learnedItemIds += question.subjectItemId
            val nextCorrect = state.correctCount + 1
            _uiState.update {
                it.copy(
                    selectedIndex = index,
                    correctCount = nextCorrect,
                )
            }
            // Pause before moving on so the child actually sees the tile turn
            // green. Advancing instantly reads as the app ignoring them, and the
            // mascot's praise lands before the next prompt replaces it.
            viewModelScope.launch {
                delay(AUTO_ADVANCE_DELAY_MS)
                onNext()
            }
        } else {
            // Gentle nudge: stay on the question, let the child try again.
            feedbackPlayer.onTryAgain()
            _uiState.update { it.copy(mistakesOnCurrent = it.mistakesOnCurrent + 1) }
        }
    }

    /**
     * Moves to the next question.
     *
     * Public so the UI can offer an explicit "next" button as well as the automatic
     * advance. A child who is unsure about the pacing is better served by a big
     * button they control.
     */
    fun onNext() {
        val state = _uiState.value
        if (state.stage != QuizStage.ACTIVE || !state.hasAnsweredCurrent) return
        if (state.isLastQuestion) {
            finish(state.correctCount)
            return
        }
        _uiState.update {
            it.copy(
                currentIndex = it.currentIndex + 1,
                selectedIndex = null,
                mistakesOnCurrent = 0,
            )
        }
        _uiState.value.currentQuestion?.let { speakPrompt(it) }
    }

    /** Hides the confetti overlay, leaving the result buttons in place. */
    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    /**
     * Clears the finished state so the destination can be rebuilt for a fresh run.
     *
     * The screen cannot simply re-run the quiz in place: the questions are built
     * once in `init` from a seed fixed in the back stack, so a replay has to be a
     * new navigation rather than a state change.
     */
    fun onClearedForReplay() {
        _uiState.update { it.copy(stage = QuizStage.ACTIVE, result = null, showCelebration = false) }
    }

    /** Replays the current question's audio. */
    fun onReplayPrompt() {
        // Tap first: the pop is 90ms and speech must not start under it, or the
        // start of the prompt is masked (see FeedbackPlayer).
        feedbackPlayer.onTap()
        _uiState.value.currentQuestion?.let { speakPrompt(it) }
    }

    private fun speakPrompt(question: QuizQuestion) {
        feedbackPlayer.pronouncePrompt(question.speakText, question.speakLocale)
    }

    private fun finish(correctCount: Int) {
        _uiState.update { it.copy(stage = QuizStage.FINISHING) }
        viewModelScope.launch {
            val total = _uiState.value.questions.size
            val result = recordQuizResult(
                moduleType = moduleType,
                itemIds = learnedItemIds.toList(),
                correctAnswers = correctCount,
                totalQuestions = total,
                today = LocalDate.now(),
            )
            if (result.starsEarned > 0) feedbackPlayer.onStarEarned()
            _uiState.update {
                it.copy(
                    stage = QuizStage.COMPLETE,
                    result = result,
                    showCelebration = true,
                )
            }
        }
    }

    private companion object {
        const val ARG_MODULE = "module"
        const val ARG_ITEM_IDS = "itemIds"
        const val ARG_QUESTION_COUNT = "questionCount"
        const val ARG_SEED = "seed"
        const val DEFAULT_QUESTION_COUNT = 5

        /** Upper bound keeps a malformed route from generating 1000 questions. */
        const val MAX_QUESTIONS = 20

        /** Long enough to register the green tile, short enough not to drag. */
        const val AUTO_ADVANCE_DELAY_MS = 1_100L
    }
}
