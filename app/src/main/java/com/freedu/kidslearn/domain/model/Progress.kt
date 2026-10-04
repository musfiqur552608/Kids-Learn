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

/** The mini-games in the Games hub. Stored by name in Room, so append only. */
enum class GameType {
    MEMORY_MATCH,
    FIND_THE_CORRECT_ONE,
    TIMED_QUIZ,
    ODD_ONE_OUT,
    ;

    companion object {
        fun fromKey(key: String): GameType =
            entries.firstOrNull { it.name == key } ?: TIMED_QUIZ
    }
}

/**
 * One day of practice: lessons touched (a lesson whose last attempt was that
 * day) and games played. Lessons are an approximation - Room keeps the
 * high-water mark per lesson, not full history - while games are exact.
 */
data class DayActivity(
    val date: java.time.LocalDate,
    val lessons: Int,
    val games: Int,
) {
    val total: Int get() = lessons + games
}

/**
 * The last seven days, oldest first, ending today. Always seven entries, even
 * with no history, so the parent-zone chart never changes shape.
 */
data class WeeklyActivity(
    val days: List<DayActivity>,
) {
    val totalLessons: Int get() = days.sumOf { it.lessons }
    val totalGames: Int get() = days.sumOf { it.games }

    companion object {
        fun empty(today: java.time.LocalDate = java.time.LocalDate.now()): WeeklyActivity =
            WeeklyActivity((6 downTo 0).map { DayActivity(today.minusDays(it.toLong()), 0, 0) })
    }
}

/**
 * Today's progress toward the parent-set daily goal.
 *
 * One number on purpose: lessons touched + games played is a single "practices"
 * count a child can hold in mind, and it needs no new history table - both
 * halves come from data the app already keeps.
 */
data class DailyGoalProgress(
    val goal: Int,
    val doneToday: Int,
) {
    val met: Boolean get() = doneToday >= goal
    val remaining: Int get() = (goal - doneToday).coerceAtLeast(0)
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
