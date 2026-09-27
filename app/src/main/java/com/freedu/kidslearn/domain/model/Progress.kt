package com.freedu.kidslearn.domain.model

import java.time.LocalDate

/**
 * Persisted progress for one lesson of one module.
 *
 * Unique on `(moduleType, itemId)` so re-playing a lesson updates the existing
 * row instead of inflating the child's history.
 */
data class LessonProgress(
    val moduleType: ModuleType,
    val itemId: String,
    val isCompleted: Boolean,
    val starsEarned: Int,
    val lastAttemptDate: LocalDate?,
) {
    companion object {
        /** Stars can never exceed this; used for both clamping and UI bars. */
        const val MAX_STARS: Int = 3
    }
}

/** One completed play-through of a mini-game. */
data class GameScore(
    val gameType: GameType,
    val score: Int,
    val starsEarned: Int,
    val playedAt: LocalDate,
)

/** The three mini-games in the Games hub. */
enum class GameType {
    MEMORY_MATCH,
    FIND_THE_CORRECT_ONE,
    TIMED_QUIZ,
    ;

    companion object {
        fun fromKey(key: String): GameType =
            entries.firstOrNull { it.name == key } ?: TIMED_QUIZ
    }
}

/**
 * Lifetime aggregates shown on the progress dashboard and in the parent zone.
 *
 * Held as a single row (see `UserStatsEntity`) rather than recomputed with
 * `SUM()` aggregates: the totals also include *coins* which are awarded
 * independently of lessons, and keeping one row makes the read trivially cheap
 * for the low-end devices this app targets.
 */
data class UserStats(
    val totalStars: Int = 0,
    val totalCoins: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: LocalDate? = null,
    val lessonsCompleted: Int = 0,
    val gamesPlayed: Int = 0,
) {
    companion object {
        val EMPTY = UserStats()
    }
}

/**
 * A badge the child can unlock.
 *
 * The *requirement* lives in the domain ([BadgeKey.isEarnedBy]) rather than being
 * scattered across call sites, so unlocking is evaluated in exactly one place:
 * `EvaluateBadgesUseCase`.
 */
enum class BadgeKey {
    FIRST_STEP,
    FIRST_PERFECT,
    ENGLISH_STAR,
    BANGLA_STAR,
    ARABIC_STAR,
    MATHS_STAR,
    COLLECTOR_10,
    COLLECTOR_25,
    GAME_CHAMPION,
    STREAK_3,
    STREAK_7,
    ;

    val key: String get() = name
}

/** A badge row as stored in (and read from) Room. */
data class Badge(
    val badgeKey: BadgeKey,
    val unlockedDate: LocalDate,
)
