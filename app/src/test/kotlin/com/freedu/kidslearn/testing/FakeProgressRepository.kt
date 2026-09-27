package com.freedu.kidslearn.testing

import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.model.LessonProgress
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UserStats
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * In-memory stand-in for the Room-backed repositories.
 *
 * ## Why the fakes are hand-written rather than generated
 * These implement the real domain interfaces and reproduce the two behaviours the
 * use cases actually depend on - the star high-water mark and idempotent badge
 * unlocking. That is the point: a generated mock would return 0 for every call and
 * quietly let a broken use case pass.
 *
 * The fake is a single class implementing both interfaces, mirroring the production
 * `LocalProgressRepository`.
 */
class FakeProgressRepository : ProgressRepository, StatsRepository {

    private val lessonRows = MutableStateFlow<Map<Pair<ModuleType, String>, LessonProgress>>(
        emptyMap(),
    )
    private val gameRows = MutableStateFlow<List<GameScore>>(emptyList())
    private val badges = MutableStateFlow<List<Badge>>(emptyList())
    private val stats = MutableStateFlow(UserStats.EMPTY)

    // ------------------------------------------------------------- ProgressRepository

    override fun observeLessonProgress(moduleType: ModuleType): Flow<List<LessonProgress>> =
        lessonRows.map { rows -> rows.values.filter { it.moduleType == moduleType } }

    override fun observeAllLessonProgress(): Flow<List<LessonProgress>> =
        lessonRows.map { rows -> rows.values.toList() }

    override suspend fun recordLesson(
        moduleType: ModuleType,
        itemId: String,
        starsEarned: Int,
        today: LocalDate,
    ) {
        val key = moduleType to itemId
        val existing = lessonRows.value[key]
        // Mirrors the SQL `MAX(stars_earned, excluded.stars_earned)` upsert.
        val best = maxOf(existing?.starsEarned ?: 0, starsEarned)
        lessonRows.value = lessonRows.value + (
            key to LessonProgress(
                moduleType = moduleType,
                itemId = itemId,
                isCompleted = true,
                starsEarned = best,
                lastAttemptDate = today,
            )
            )
        if (existing?.isCompleted != true) {
            stats.value = stats.value.copy(
                lessonsCompleted = stats.value.lessonsCompleted + 1,
            )
        }
    }

    override suspend fun recordGame(result: GameScore): Boolean {
        val previousBest = gameRows.value.filter { it.gameType == result.gameType }
            .maxOfOrNull { it.score } ?: 0
        gameRows.value = gameRows.value + result
        return result.score > previousBest
    }

    override fun observeBestScore(gameType: GameType): Flow<Int> =
        gameRows.map { rows -> rows.filter { it.gameType == gameType }.maxOfOrNull { it.score } ?: 0 }

    override fun observeRecentScores(limit: Int): Flow<List<GameScore>> =
        gameRows.map { rows -> rows.sortedByDescending { it.playedAt }.take(limit) }

    override suspend fun clearAll() {
        lessonRows.value = emptyMap()
        gameRows.value = emptyList()
    }

    // -------------------------------------------------------------- StatsRepository

    override fun observeStats(): Flow<UserStats> = stats

    override fun observeBadges(): Flow<List<Badge>> = badges

    override fun observeModuleProgress(): Flow<List<ModuleProgress>> =
        lessonRows.map { rows ->
            ModuleType.entries.map { module ->
                val forModule = rows.values.filter { it.moduleType == module }
                ModuleProgress(
                    moduleType = module,
                    totalItems = 26,
                    completedItems = forModule.count { it.isCompleted },
                    totalStars = forModule.sumOf { it.starsEarned },
                    maxStars = 78,
                )
            }
        }

    override fun observeDashboard(): Flow<DashboardSnapshot> =
        kotlinx.coroutines.flow.combine(
            stats,
            observeModuleProgress(),
            badges,
            gameRows,
        ) { s, modules, b, scores ->
            DashboardSnapshot(stats = s, modules = modules, badges = b, recentScores = scores)
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
        val existing = badges.value.map { it.badgeKey }.toSet()
        val fresh = keys.filterNot { it in existing }
        badges.value = badges.value + fresh.map { Badge(it, today) }
        return fresh
    }

    override suspend fun resetAll() {
        stats.value = UserStats.EMPTY
        badges.value = emptyList()
    }

    // -------------------------------------------------------------- test helpers

    /** Seeds stars and lessons so badge rules can be exercised. */
    fun seedStats(
        totalStars: Int = 0,
        totalCoins: Int = 0,
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        lessonsCompleted: Int = 0,
        gamesPlayed: Int = 0,
    ) {
        stats.value = UserStats(
            totalStars = totalStars,
            totalCoins = totalCoins,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lessonsCompleted = lessonsCompleted,
            gamesPlayed = gamesPlayed,
        )
    }

    /** Seeds a module's star total, which is what module badges read. */
    fun seedModuleStars(starsByModule: Map<ModuleType, Int>) {
        val rows = starsByModule.entries.flatMap { (module, stars) ->
            // Spread the stars over enough synthetic lessons to reach the total.
            (0 until stars).map { index ->
                (module to "seed_${module.name}_$index") to LessonProgress(
                    moduleType = module,
                    itemId = "seed_${module.name}_$index",
                    isCompleted = true,
                    starsEarned = 1,
                    lastAttemptDate = LocalDate.of(2026, 1, 1),
                )
            }
        }.toMap()
        lessonRows.value = rows
    }

    fun currentBadges(): List<BadgeKey> = badges.value.map { it.badgeKey }

    fun currentStats(): UserStats = stats.value

    fun currentLesson(module: ModuleType, itemId: String): LessonProgress? =
        lessonRows.value[module to itemId]
}
