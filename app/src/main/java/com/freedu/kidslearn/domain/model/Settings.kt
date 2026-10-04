package com.freedu.kidslearn.domain.model

import java.time.LocalDate

/** The languages the app's own chrome can be displayed in. */
enum class UiLanguage(val tag: String) {
    ENGLISH("en"),
    BANGLA("bn"),
    ;

    companion object {
        fun fromTag(tag: String?): UiLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

/** Parent-selectable appearance. */
enum class ThemeMode {
    LIGHT,
    DARK,
}

/**
 * User preferences backed by Preferences DataStore.
 *
 * DataStore (not Room) because these are single scalar values, written rarely and
 * read on every screen - DataStore's `Flow` API makes that cheap and avoids
 * writing a table row per toggle.
 */
data class AppSettings(
    val uiLanguage: UiLanguage = UiLanguage.ENGLISH,
    val soundEnabled: Boolean = true,
    val childName: String = "",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    /** "Continue where you left off" on Home. */
    val lastModule: ModuleType? = null,
    /** When true, the parent zone is behind a maths gate. */
    val parentGateEnabled: Boolean = true,
    /**
     * Practices (lessons touched + games played) the child aims for each day.
     * Always within [MIN_DAILY_GOAL]..[MAX_DAILY_GOAL]; setters coerce.
     */
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    /** Last date the goal celebration fired, so it fires once per day. */
    val goalCelebratedDate: LocalDate? = null,
) {
    companion object {
        val DEFAULT = AppSettings()

        const val DEFAULT_DAILY_GOAL = 3
        const val MIN_DAILY_GOAL = 1
        const val MAX_DAILY_GOAL = 8
    }
}

/** Result of a finished quiz, handed to the result screen. */
data class QuizResult(
    val moduleType: ModuleType,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val starsEarned: Int,
    val coinsEarned: Int,
    val itemIds: List<String>,
    val isNewPersonalBest: Boolean = false,
    /**
     * Badges unlocked by this exact run, for the result screen to celebrate.
     * Without this the badge system would be write-only from the child's
     * perspective: the row lands in Room and nothing on screen ever mentions it.
     */
    val newBadges: List<BadgeKey> = emptyList(),
) {
    val accuracy: Float
        get() = if (totalQuestions == 0) 0f else correctAnswers.toFloat() / totalQuestions
}

/** Result of a finished mini-game. */
data class GameResult(
    val gameType: GameType,
    val score: Int,
    val totalRounds: Int,
    val correctAnswers: Int,
    val starsEarned: Int,
    val coinsEarned: Int,
    val isNewPersonalBest: Boolean = false,
    /** Badges unlocked by this exact run - see [QuizResult.newBadges]. */
    val newBadges: List<BadgeKey> = emptyList(),
)

/** Per-module roll-up for the progress dashboard. */
data class ModuleProgress(
    val moduleType: ModuleType,
    val totalItems: Int,
    val completedItems: Int,
    val totalStars: Int,
    val maxStars: Int,
) {
    val completionFraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems

    val completionPercent: Int
        get() = (completionFraction * 100).toInt().coerceIn(0, 100)
}

/** Everything the parent zone needs in one shot. */
data class DashboardSnapshot(
    val stats: UserStats = UserStats.EMPTY,
    val modules: List<ModuleProgress> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val recentScores: List<GameScore> = emptyList(),
    val totalItems: Int = 0,
    val today: LocalDate = LocalDate.now(),
) {
    val overallPercent: Int
        get() = if (totalItems == 0) 0 else {
            val done = modules.sumOf { it.completedItems }
            ((done.toFloat() / totalItems) * 100).toInt().coerceIn(0, 100)
        }
}
