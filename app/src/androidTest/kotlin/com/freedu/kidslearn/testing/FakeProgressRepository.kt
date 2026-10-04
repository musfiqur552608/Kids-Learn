package com.freedu.kidslearn.testing

import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.model.LessonProgress
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.data.repository.bucketWeekly
import com.freedu.kidslearn.domain.model.UserStats
import com.freedu.kidslearn.domain.model.WeeklyActivity
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * In-memory repositories for instrumented tests.
 *
 * A copy of the unit-test fake rather than a shared module: the `test` and
 * `androidTest` source sets do not share code without an extra Gradle plugin, and
 * duplicating ~120 lines is cheaper than adding a publishing configuration to an
 * app that will never need one.
 */
class FakeProgressRepository : ProgressRepository, StatsRepository {

    private val lessons = MutableStateFlow<List<LessonProgress>>(emptyList())
    private val games = MutableStateFlow<List<GameScore>>(emptyList())
    private val badgeRows = MutableStateFlow<List<Badge>>(emptyList())
    private val stats = MutableStateFlow(UserStats.EMPTY)

    override fun observeLessonProgress(moduleType: ModuleType): Flow<List<LessonProgress>> =
        lessons.map { rows -> rows.filter { it.moduleType == moduleType } }

    override fun observeAllLessonProgress(): Flow<List<LessonProgress>> = lessons

    override suspend fun recordLesson(
        moduleType: ModuleType,
        itemId: String,
        starsEarned: Int,
        today: LocalDate,
    ) {
        val index = lessons.value.indexOfFirst { it.moduleType == moduleType && it.itemId == itemId }
        val existing = lessons.value.getOrNull(index)
        val updated = LessonProgress(
            moduleType = moduleType,
            itemId = itemId,
            isCompleted = true,
            starsEarned = maxOf(existing?.starsEarned ?: 0, starsEarned),
            lastAttemptDate = today,
        )
        lessons.value = lessons.value.toMutableList().apply {
            if (index >= 0) set(index, updated) else add(updated)
        }
    }

    override suspend fun recordGame(result: GameScore): Boolean {
        games.value = games.value + result
        return true
    }

    override fun observeBestScore(gameType: GameType): Flow<Int> =
        games.map { rows -> rows.filter { it.gameType == gameType }.maxOfOrNull { it.score } ?: 0 }

    override fun observeRecentScores(limit: Int): Flow<List<GameScore>> =
        games.map { rows -> rows.takeLast(limit).reversed() }

    override suspend fun clearAll() {
        lessons.value = emptyList()
        games.value = emptyList()
    }

    override fun observeStats(): Flow<UserStats> = stats

    override fun observeBadges(): Flow<List<Badge>> = badgeRows

    override fun observeModuleProgress(): Flow<List<ModuleProgress>> =
        lessons.map { rows ->
            ModuleType.entries.map { module ->
                val forModule = rows.filter { it.moduleType == module }
                ModuleProgress(
                    moduleType = module,
                    totalItems = 26,
                    completedItems = forModule.size,
                    totalStars = forModule.sumOf { it.starsEarned },
                    maxStars = 78,
                )
            }
        }

    override fun observeDashboard(): Flow<DashboardSnapshot> = combine(
        stats,
        observeModuleProgress(),
        badgeRows,
        games,
    ) { s, modules, b, scores ->
        DashboardSnapshot(stats = s, modules = modules, badges = b, recentScores = scores)
    }

    override fun observeWeeklyActivity(today: LocalDate): Flow<WeeklyActivity> =
        combine(lessons, games) { lessonRows, gameRows ->
            bucketWeekly(
                lessonDates = lessonRows.map { it.lastAttemptDate },
                gameDates = gameRows.map { it.playedAt },
                today = today,
            )
        }

    override suspend fun touchActivity(today: LocalDate): UserStats = stats.value

    override suspend fun addStars(stars: Int) {
        stats.value = stats.value.copy(totalStars = stats.value.totalStars + stars)
    }

    override suspend fun addCoins(coins: Int) {
        stats.value = stats.value.copy(totalCoins = stats.value.totalCoins + coins)
    }

    override suspend fun noteGamePlayed() {
        stats.value = stats.value.copy(gamesPlayed = stats.value.gamesPlayed + 1)
    }

    override suspend fun unlockBadges(keys: List<BadgeKey>, today: LocalDate): List<BadgeKey> {
        val fresh = keys.filterNot { key -> badgeRows.value.any { it.badgeKey == key } }
        badgeRows.value = badgeRows.value + fresh.map { Badge(it, today) }
        return fresh
    }

    override suspend fun resetAll() {
        stats.value = UserStats.EMPTY
        badgeRows.value = emptyList()
    }
}
