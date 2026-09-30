package com.freedu.kidslearn.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.GameResult
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.usecase.RecordGameResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random
import javax.inject.Inject

// Game-wide constants live at file scope because they are also used as default
// values in the state data classes above, and a `private companion` of a class
// further down the file is not in scope there.
private const val ROUNDS = 8
private const val OPTION_COUNT = 4
private const val TOTAL_QUESTIONS = 8
private const val SECONDS_PER_QUESTION = 20

// ============================================================================
// Memory match
// ============================================================================

/** One face-down-or-up card on the board. */
data class MemoryCardUi(
    val id: Int,
    /** Stable identity of the pair, e.g. `"ENGLISH_A"`. */
    val pairKey: String,
    val glyph: String,
    val isFaceUp: Boolean,
    val isMatched: Boolean,
)

data class MemoryUiState(
    val cards: List<MemoryCardUi> = emptyList(),
    val taps: Int = 0,
    val matchedPairs: Int = 0,
    val firstSelectedId: Int? = null,
    val secondSelectedId: Int? = null,
    val result: GameResult? = null,
    val showCelebration: Boolean = false,
    val bestScore: Int = 0,
) {
    val totalPairs: Int get() = cards.size / 2
    val isComplete: Boolean get() = cards.isNotEmpty() && matchedPairs == cards.size / 2
}

/**
 * Memory match: flip two cards, find the pairs.
 *
 * ## Why the cards are letters, not pictures
 * Matching letter-to-picture would be a *reading* test, which is a different skill
 * from visual memory and would confound the game. Pairing a letter with its
 * lowercase twin tests recall without adding a decoding step the child may not
 * have.
 *
 * ## Flip-back timing
 * A wrong pair stays visible for [FLIP_BACK_DELAY_MS] then turns over. Without
 * that delay a child never registers that the pair was wrong; without a "wrong"
 * sound or message, which would violate the positive-only rule, the delay is the
 * only feedback there is.
 */
@HiltViewModel
class MemoryMatchViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    private val recordGameResult: RecordGameResultUseCase,
    private val feedbackPlayer: FeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    val bestScore: StateFlow<Int> = progressRepository
        .observeBestScore(GameType.MEMORY_MATCH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var flipBackJob: Job? = null

    init {
        startNewGame()
    }

    /** Hides the confetti overlay without discarding the finished result. */
    fun dismissResult() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    fun startNewGame() {
        flipBackJob?.cancel()
        // A fresh Random per game, so two games in a row never deal the same board.
        val random = Random(System.nanoTime())
        val pool = contentRepository.letters(ModuleType.ENGLISH)
        val chosen = pool.shuffled(random).take(PAIR_COUNT)

        // Two cards per letter. `pairKey` is what the game matches on, so the two
        // copies are interchangeable; the layout is decided by the shuffle, and
        // ids are assigned afterwards so they line up with board positions.
        val deck = chosen.flatMap { letter ->
            val glyph = letter.letter + letter.secondary
            listOf(
                MemoryCardUi(
                    id = 0,
                    pairKey = letter.id,
                    glyph = glyph,
                    isFaceUp = false,
                    isMatched = false,
                ),
                MemoryCardUi(
                    id = 0,
                    pairKey = letter.id,
                    glyph = glyph,
                    isFaceUp = false,
                    isMatched = false,
                ),
            )
        }

        val cards = deck
            .shuffled(random)
            .mapIndexed { index, card -> card.copy(id = index) }

        _uiState.value = MemoryUiState(cards = cards, bestScore = bestScore.value)
    }

    fun onCardTapped(id: Int) {
        val state = _uiState.value
        val card = state.cards.getOrNull(id) ?: return
        // Already matched, already face up, or a pair is still being judged.
        if (card.isMatched || card.isFaceUp) return
        if (state.secondSelectedId != null) return

        feedbackPlayer.onTap()

        if (state.firstSelectedId == null) {
            _uiState.update { it.copy(firstSelectedId = id, taps = it.taps + 1) }
            _uiState.value.cards.getOrNull(id)?.let { reveal(it) }
            return
        }

        val first = state.cards.getOrNull(state.firstSelectedId) ?: return
        reveal(card)
        _uiState.update { it.copy(secondSelectedId = id, taps = it.taps + 1) }

        if (first.pairKey == card.pairKey) {
            feedbackPlayer.onCorrect()
            _uiState.update { current ->
                current.copy(matchedPairs = current.matchedPairs + 1)
            }
            // Flip the pair back down: a matched card shows as a "done" tile,
            // which is more informative than leaving it open.
            _uiState.update { current ->
                current.copy(
                    firstSelectedId = null,
                    secondSelectedId = null,
                    cards = current.cards.map {
                        if (it.pairKey == card.pairKey) it.copy(isMatched = true, isFaceUp = false) else it
                    },
                )
            }
            if (_uiState.value.isComplete) finish()
        } else {
            // Gentle, non-judgemental feedback - the two cards simply turn back.
            feedbackPlayer.onTryAgain()
            flipBackJob?.cancel()
            flipBackJob = viewModelScope.launch {
                delay(FLIP_BACK_DELAY_MS)
                val ids = listOfNotNull(_uiState.value.firstSelectedId, _uiState.value.secondSelectedId)
                _uiState.value = _uiState.value.copy(
                    firstSelectedId = null,
                    secondSelectedId = null,
                    cards = _uiState.value.cards.map {
                        if (it.id in ids && !it.isMatched) it.copy(isFaceUp = false) else it
                    },
                )
            }
        }
    }

    private fun reveal(card: MemoryCardUi) {
        _uiState.update { current ->
            current.copy(
                cards = current.cards.map { if (it.id == card.id) it.copy(isFaceUp = true) else it },
            )
        }
    }

    private fun finish() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = recordGameResult(
                gameType = GameType.MEMORY_MATCH,
                score = state.matchedPairs,
                totalRounds = state.totalPairs,
                correctAnswers = state.matchedPairs,
                today = LocalDate.now(),
            )
            _uiState.update {
                it.copy(
                    result = result,
                    showCelebration = true,
                    bestScore = bestScore.value,
                )
            }
            feedbackPlayer.onModuleComplete()
        }
    }

    private companion object {
        /** Six pairs = twelve cards: a 3x4 grid, which fits a phone without scrolling. */
        const val PAIR_COUNT = 6
        const val FLIP_BACK_DELAY_MS = 1_400L
    }
}

// ============================================================================
// Find the correct one
// ============================================================================

data class FindCorrectUiState(
    val targetGlyph: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val round: Int = 0,
    val totalRounds: Int = ROUNDS,
    val correctAnswers: Int = 0,
    val mistakes: Int = 0,
    val result: GameResult? = null,
    val showCelebration: Boolean = false,
) {
    val isComplete: Boolean get() = round >= totalRounds
}

/**
 * "Find the correct one": four options, one of which matches the target.
 *
 * Distractors are drawn from the same alphabet as the target, so the child has to
 * discriminate between similar letters rather than spot the odd one out. All four
 * options are re-shuffled every round, because a child who learns option A is
 * always right stops playing the game.
 */
@HiltViewModel
class FindCorrectViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val recordGameResult: RecordGameResultUseCase,
    private val feedbackPlayer: FeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FindCorrectUiState())
    val uiState: StateFlow<FindCorrectUiState> = _uiState.asStateFlow()

    init {
        nextRound()
    }

    fun onOptionTapped(index: Int) {
        val state = _uiState.value
        if (state.isComplete) return

        feedbackPlayer.onTap()
        if (index == state.correctIndex) {
            feedbackPlayer.onCorrect()
            _uiState.update {
                it.copy(
                    correctAnswers = it.correctAnswers + 1,
                    round = it.round + 1,
                )
            }
            if (_uiState.value.isComplete) {
                finish()
            } else {
                nextRoundKeepingScore()
            }
        } else {
            feedbackPlayer.onTryAgain()
            _uiState.update { it.copy(mistakes = it.mistakes + 1) }
        }
    }

    /** Hides the confetti overlay without discarding the finished result. */
    fun dismissResult() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    fun playAgain() {
        _uiState.value = FindCorrectUiState()
        nextRound()
    }

    private fun nextRound() {
        _uiState.update { it.copy(round = 0, correctAnswers = 0, mistakes = 0) }
        nextRoundKeepingScore()
    }

    private fun nextRoundKeepingScore() {
        val random = Random(System.nanoTime())
        val pool = contentRepository.letters(ModuleType.ENGLISH)
        val target = pool[random.nextInt(pool.size)]
        val distractors = pool.filter { it.letter != target.letter }
            .shuffled(random)
            .take(OPTION_COUNT - 1)
        val options = (distractors.map { it.letter } + target.letter).shuffled(random)
        _uiState.update {
            it.copy(
                targetGlyph = target.letter,
                options = options,
                correctIndex = options.indexOf(target.letter),
            )
        }
    }

    private fun finish() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = recordGameResult(
                gameType = GameType.FIND_THE_CORRECT_ONE,
                score = state.correctAnswers,
                totalRounds = state.totalRounds,
                correctAnswers = state.correctAnswers,
                today = LocalDate.now(),
            )
            _uiState.update { it.copy(result = result, showCelebration = true) }
            feedbackPlayer.onModuleComplete()
        }
    }


}

// ============================================================================
// Timed quiz
// ============================================================================

data class TimedQuizUiState(
    val question: QuizQuestion? = null,
    val questionNumber: Int = 0,
    val totalQuestions: Int = TOTAL_QUESTIONS,
    val secondsLeft: Int = SECONDS_PER_QUESTION,
    val score: Int = 0,
    val isRunning: Boolean = false,
    val isOver: Boolean = false,
    val result: GameResult? = null,
    val showCelebration: Boolean = false,
)

/**
 * A countdown quiz: answer as many as you can before each question's timer runs out.
 *
 * ## Why the timer is per-question and generous
 * 20 seconds is long enough to read the prompt aloud, look at four options and
 * tap. A shared countdown across the whole round would mean a child who
 * deliberates on question one has no time left for question eight - which
 * measures the child's anxiety rather than their maths.
 *
 * A missed question is scored as zero and moves on. There is no penalty, no buzzer
 * and no "out of time" message beyond a neutral one.
 */
@HiltViewModel
class TimedQuizViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val recordGameResult: RecordGameResultUseCase,
    private val feedbackPlayer: FeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimedQuizUiState())
    val uiState: StateFlow<TimedQuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var questions: List<QuizQuestion> = emptyList()
    private var answeredCorrectly = 0

    init {
        startRound()
    }

    /** Hides the confetti overlay without discarding the finished result. */
    fun dismissResult() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    fun startRound() {
        timerJob?.cancel()
        questions = contentRepository.buildQuiz(
            moduleType = ModuleType.MATHS,
            itemIds = emptyList(),
            count = TOTAL_QUESTIONS,
            seed = System.currentTimeMillis(),
        )
        answeredCorrectly = 0
        _uiState.value = TimedQuizUiState(
            question = questions.firstOrNull(),
            questionNumber = 0,
            isRunning = true,
        )
        startTimer()
    }

    fun onAnswerSelected(index: Int) {
        val state = _uiState.value
        val question = state.question ?: return
        if (!state.isRunning) return

        timerJob?.cancel()

        if (index == question.correctIndex) {
            feedbackPlayer.onCorrect()
            answeredCorrectly++
            // A correct answer is worth more the faster it came, but never more
            // than 3 points, so speed can never beat understanding.
            val speedBonus = (state.secondsLeft / SECONDS_PER_QUESTION).coerceIn(0, 2)
            _uiState.update { it.copy(score = it.score + 1 + speedBonus) }
        } else {
            feedbackPlayer.onTryAgain()
        }

        val nextIndex = state.questionNumber + 1
        if (nextIndex >= questions.size) {
            finish()
        } else {
            _uiState.update {
                it.copy(
                    question = questions[nextIndex],
                    questionNumber = nextIndex,
                    secondsLeft = SECONDS_PER_QUESTION,
                    isRunning = true,
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = SECONDS_PER_QUESTION
            while (isActive && remaining > 0) {
                delay(1_000)
                remaining--
                // A tick in the last three seconds is a *countdown cue*, not a
                // warning: no vibration, no red flash, nothing that would frighten.
                if (remaining <= 3) feedbackPlayer.onCountdownTick()
                _uiState.update { it.copy(secondsLeft = remaining) }
            }
            if (isActive && remaining == 0) {
                _uiState.update { it.copy(isOver = true, isRunning = false) }
                feedbackPlayer.onTryAgain()
            }
        }
    }

    private fun finish() {
        viewModelScope.launch {
            val result = recordGameResult(
                gameType = GameType.TIMED_QUIZ,
                score = _uiState.value.score,
                totalRounds = questions.size,
                correctAnswers = answeredCorrectly,
                today = LocalDate.now(),
            )
            _uiState.update {
                it.copy(
                    isOver = true,
                    isRunning = false,
                    result = result,
                    showCelebration = true,
                )
            }
            feedbackPlayer.onModuleComplete()
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
    }


}
