package com.freedu.kidslearn.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.DailyGoalProgress
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.ui.alphabet.tileEmoji
import com.freedu.kidslearn.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A module tile on the Home screen. */
data class ModuleTileUi(
    val moduleType: ModuleType,
    val emoji: String,
    val completed: Int,
    val total: Int,
)

/**
 * State for the Home screen.
 *
 * @param showStreakPrompt true when the child's streak has just reached a
 *   milestone, which is the one moment a Home-screen banner is worth showing.
 */
data class HomeUiState(
    val childName: String = "",
    val lastModule: ModuleType? = null,
    val tiles: List<ModuleTileUi> = emptyList(),
    val totalStars: Int = 0,
    val totalCoins: Int = 0,
    val currentStreak: Int = 0,
    val gamesPlayed: Int = 0,
    val showStreakPrompt: Boolean = false,
    val dailyGoal: DailyGoalProgress = DailyGoalProgress(AppSettings.DEFAULT_DAILY_GOAL, 0),
    val settings: AppSettings = AppSettings.DEFAULT,
    val isLoading: Boolean = true,
) {
    val greeting: String? get() = childName.takeIf { it.isNotBlank() }
}

/**
 * Backing state for Home.
 *
 * Combines the dashboard snapshot with settings into one flow so the screen
 * recomposes once per change rather than once per source. On a low-end device
 * three independent collectors of Room-backed flows would cause three separate
 * recomposition passes over the whole tree on every lesson completion.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val feedbackPlayer: FeedbackPlayer,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        statsRepository.observeDashboard(),
        settingsRepository.settings,
        statsRepository.observeWeeklyActivity(LocalDate.now()),
    ) { dashboard, settings, weekly ->
        val milestones = setOf(3, 7, 14, 30, 100)
        HomeUiState(
            childName = settings.childName,
            lastModule = settings.lastModule,
            tiles = ModuleType.entries.map { module ->
                val progress = dashboard.modules.firstOrNull { it.moduleType == module }
                ModuleTileUi(
                    moduleType = module,
                    emoji = module.tileEmoji(),
                    completed = progress?.completedItems ?: 0,
                    total = progress?.totalItems ?: 0,
                )
            },
            totalStars = dashboard.stats.totalStars,
            totalCoins = dashboard.stats.totalCoins,
            currentStreak = dashboard.stats.currentStreak,
            gamesPlayed = dashboard.stats.gamesPlayed,
            // Only celebrate an *exact* milestone, so the banner appears once per
            // milestone rather than on every day the streak is 7 or more.
            showStreakPrompt = dashboard.stats.currentStreak in milestones &&
                dashboard.stats.longestStreak == dashboard.stats.currentStreak,
            dailyGoal = DailyGoalProgress(
                goal = settings.dailyGoal,
                doneToday = weekly.days.lastOrNull()?.total ?: 0,
            ),
            settings = settings,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    init {
        // Fire the goal celebration exactly once per day: when the banner flips
        // to met and no celebration is recorded for today yet. The settings
        // write flips the flag, so this cannot repeat within the day.
        viewModelScope.launch {
            uiState.map { it.dailyGoal.met && it.settings.goalCelebratedDate != LocalDate.now() }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    feedbackPlayer.onStarEarned()
                    settingsRepository.markGoalCelebrated(LocalDate.now())
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
