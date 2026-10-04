package com.freedu.kidslearn.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** A game on the hub, with the child's personal best. */
data class GameCardUi(
    val gameType: GameType,
    val bestScore: Int,
)

data class GamesHubUiState(
    val games: List<GameCardUi> = emptyList(),
    val recent: List<GameScore> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * The Games hub.
 *
 * Personal bests are shown rather than a leaderboard. The app has no network and
 * no accounts by design, so there is nothing to compare a score against except the
 * child's own past self - which is a meaningful target for this age group and the
 * strongest motivator a five-year-old responds to.
 */
@HiltViewModel
class GamesHubViewModel @Inject constructor(
    progressRepository: ProgressRepository,
) : ViewModel() {

    /**
     * `combine` with four sources.
     *
     * The per-game best scores are combined first, then folded with the recent
     * scores, rather than calling `combine` with a `List<Flow<Int>>`. The vararg
     * `combine` overload would need all flows to share one element type and returns
     * an array transform, which reads worse than two small combines - and the
     * two-stage version is also cheaper, because the inner combine is only
     * recomputed when a *score* changes.
     */
    private val bestScores = combine(
        progressRepository.observeBestScore(GameType.MEMORY_MATCH),
        progressRepository.observeBestScore(GameType.FIND_THE_CORRECT_ONE),
        progressRepository.observeBestScore(GameType.TIMED_QUIZ),
        progressRepository.observeBestScore(GameType.ODD_ONE_OUT),
    ) { scores -> scores.toList() }

    val uiState: StateFlow<GamesHubUiState> = combine(
        bestScores,
        progressRepository.observeRecentScores(RECENT_LIMIT),
    ) { bests, recent ->
        GamesHubUiState(
            games = listOf(
                GameCardUi(GameType.MEMORY_MATCH, bests[0]),
                GameCardUi(GameType.FIND_THE_CORRECT_ONE, bests[1]),
                GameCardUi(GameType.TIMED_QUIZ, bests[2]),
                GameCardUi(GameType.ODD_ONE_OUT, bests[3]),
            ),
            recent = recent,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = GamesHubUiState(),
    )

    private companion object {
        const val RECENT_LIMIT = 5
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
