package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.UserStats
import javax.inject.Inject

/**
 * Single source of truth for badge requirements.
 *
 * Keeping the rules as one pure function (instead of sprinkling `if` statements
 * across use cases) means:
 *  * a badge can never be unlocked twice by two different code paths, and
 *  * the rules are trivially reviewable and unit-testable.
 *
 * Rules are evaluated against an [AchievementSnapshot] so the function stays pure
 * and does not need to know whether the numbers came from Room, DataStore or a
 * test double.
 */
class EvaluateBadgesUseCase @Inject constructor() {

    /**
     * @param moduleProgress per-module roll-up; only the star totals are used
     * @param stats lifetime totals from `UserStats`
     * @param alreadyUnlocked keys currently present in Room, so nothing re-fires
     * @return the keys that are *newly* satisfied, in a stable order
     */
    operator fun invoke(
        stats: UserStats,
        moduleProgress: List<ModuleProgress>,
        alreadyUnlocked: Set<BadgeKey>,
    ): List<BadgeKey> = BadgeKey.entries.filter { key ->
        key !in alreadyUnlocked && key.isEarnedBy(stats, moduleProgress)
    }

    private fun BadgeKey.isEarnedBy(stats: UserStats, modules: List<ModuleProgress>): Boolean {
        fun stars(module: ModuleType) = modules.firstOrNull { it.moduleType == module }?.totalStars ?: 0
        return when (this) {
            BadgeKey.FIRST_STEP -> stats.lessonsCompleted >= 1
            BadgeKey.FIRST_PERFECT -> stats.totalStars >= FIRST_PERFECT_STARS
            BadgeKey.ENGLISH_STAR -> stars(ModuleType.ENGLISH) >= MODULE_BADGE_STARS
            BadgeKey.BANGLA_STAR -> stars(ModuleType.BANGLA) >= MODULE_BADGE_STARS
            BadgeKey.ARABIC_STAR -> stars(ModuleType.ARABIC) >= MODULE_BADGE_STARS
            BadgeKey.MATHS_STAR -> stars(ModuleType.MATHS) >= MODULE_BADGE_STARS
            BadgeKey.COLLECTOR_10 -> stats.totalStars >= 10
            BadgeKey.COLLECTOR_25 -> stats.totalStars >= 25
            BadgeKey.GAME_CHAMPION -> stats.gamesPlayed >= 5
            BadgeKey.STREAK_3 -> stats.longestStreak >= 3
            BadgeKey.STREAK_7 -> stats.longestStreak >= 7
        }
    }

    companion object {
        /** A module badge needs this many stars in that module. */
        const val MODULE_BADGE_STARS = 10

        /**
         * "First perfect" is awarded as soon as a lesson scores this many stars.
         *
         * Two, not three, on purpose: a 3-star award needs a 5+ question paper
         * (see `CalculateStarsUseCase.MIN_QUESTIONS_FOR_STARS`), so a child working
         * through single letters one at a time could not collect this badge for
         * many lessons. Awarding it at two stars makes the very first badge the
         * child can reach actually reachable.
         */
        const val FIRST_PERFECT_STARS = 2
    }
}
