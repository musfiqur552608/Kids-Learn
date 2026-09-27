package com.freedu.kidslearn.domain.repository

import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UserStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Aggregated, derived statistics.
 *
 * Kept separate from [ProgressRepository] because this is a *read* model - the
 * dashboard, the parent zone and the celebration logic all consume it, whereas
 * [ProgressRepository] is only ever written to. That split also means a
 * `ViewModel` that only draws the dashboard never takes a dependency that can
 * mutate a child's history.
 */
interface StatsRepository {
    fun observeStats(): Flow<UserStats>

    fun observeBadges(): Flow<List<Badge>>

    /** Per-module completion roll-up: needs both Room progress and the content catalog. */
    fun observeModuleProgress(): Flow<List<ModuleProgress>>

    /** Everything the progress dashboard and parent zone render, in one emission. */
    fun observeDashboard(): Flow<DashboardSnapshot>

    /**
     * Records that the app was opened today and updates the daily streak.
     *
     * Idempotent per calendar day, so it is safe to call from
     * `Application.onCreate` *and* on every Home resume.
     */
    suspend fun touchActivity(today: LocalDate): UserStats

    suspend fun addStars(stars: Int)

    suspend fun addCoins(coins: Int)

    suspend fun noteGamePlayed()

    /**
     * Unlocks [keys] that are not already unlocked (an `INSERT OR IGNORE`, so this
     * is naturally idempotent) and returns the keys that were actually new.
     */
    suspend fun unlockBadges(keys: List<BadgeKey>, today: LocalDate): List<BadgeKey>

    suspend fun resetAll()
}
