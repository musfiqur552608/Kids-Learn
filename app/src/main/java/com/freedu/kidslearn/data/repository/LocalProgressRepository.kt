package com.freedu.kidslearn.data.repository

import com.freedu.kidslearn.data.local.dao.BadgeDao
import com.freedu.kidslearn.data.local.dao.GameScoreDao
import com.freedu.kidslearn.data.local.dao.LessonProgressDao
import com.freedu.kidslearn.data.local.dao.UserStatsDao
import com.freedu.kidslearn.data.local.entity.BadgeEntity
import com.freedu.kidslearn.data.local.entity.GameScoreEntity
import com.freedu.kidslearn.data.local.entity.UserStatsEntity
import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.DayActivity
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.WeeklyActivity
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.model.LessonProgress
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UserStats
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import com.freedu.kidslearn.domain.usecase.CalculateStreakUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local date codec shared by every entity in this package.
 *
 * Dates are stored as ISO-8601 `yyyy-MM-dd` strings rather than epoch millis.
 * A daily streak is a *calendar* concept, and persisting the local date means a
 * child who changes timezone overnight does not silently lose a day of history.
 * The column is a `String` in Room for exactly this reason - the database is
 * inspected by hand during support, and `2026-09-27` is readable while `1790000
 * ...` is not.
 */
internal object DateCodec {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun encode(date: LocalDate): String = date.format(FORMAT)

    /** Blank or malformed values decode to null instead of throwing. */
    fun decode(value: String?): LocalDate? =
        value?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it, FORMAT) }.getOrNull() }
}

/**
 * Room-backed implementation of both write-side repositories.
 *
 * ## One class, two interfaces
 * [ProgressRepository] and [StatsRepository] are separate *domain* interfaces so
 * each use case depends only on what it actually needs, but they share one
 * implementation because they share the same DAOs, the same date codec and the
 * same entity-to-domain mappers. Splitting them into two classes would duplicate
 * all three for no isolation benefit.
 *
 * ## Threading
 * Every DAO call is `suspend` or returns a `Flow`, so Room dispatches the work to
 * its own executor. Nothing in this class can touch the main thread.
 */
@Singleton
class LocalProgressRepository @Inject constructor(
    private val lessonProgressDao: LessonProgressDao,
    private val gameScoreDao: GameScoreDao,
    private val badgeDao: BadgeDao,
    private val userStatsDao: UserStatsDao,
    private val contentRepository: ContentRepository,
    private val calculateStreak: CalculateStreakUseCase,
) : ProgressRepository, StatsRepository {

    // ---------------------------------------------------------------- progress

    override fun observeLessonProgress(moduleType: ModuleType): Flow<List<LessonProgress>> =
        lessonProgressDao.observeByModule(moduleType.name)
            .map { rows -> rows.mapNotNull(::toDomain) }

    override fun observeAllLessonProgress(): Flow<List<LessonProgress>> =
        lessonProgressDao.observeAll().map { rows -> rows.mapNotNull(::toDomain) }

    override suspend fun recordLesson(
        moduleType: ModuleType,
        itemId: String,
        starsEarned: Int,
        today: LocalDate,
    ) {
        val clamped = starsEarned.coerceIn(0, LessonProgress.MAX_STARS)
        val encoded = DateCodec.encode(today)

        // Read first: `lessons_completed` must count *distinct* lessons, so a repeat
        // attempt must not increment it. The `recordResult` upsert itself is
        // atomic in SQL, so this read only affects the counter, not the stars.
        val alreadyCompleted = lessonProgressDao.find(moduleType.name, itemId)?.isCompleted == true

        lessonProgressDao.recordResult(
            moduleType = moduleType.name,
            itemId = itemId,
            starsEarned = clamped,
            today = encoded,
        )

        if (!alreadyCompleted) {
            userStatsDao.ensureRow()
            userStatsDao.addLessonsCompleted(1)
        }
    }

    override suspend fun recordGame(result: GameScore): Boolean {
        val isBest = result.score > gameScoreDao.observeBestScore(result.gameType.name).first()
        gameScoreDao.insert(
            GameScoreEntity(
                gameType = result.gameType.name,
                score = result.score,
                starsEarned = result.starsEarned,
                playedAt = DateCodec.encode(result.playedAt),
            ),
        )
        return isBest
    }

    override fun observeBestScore(gameType: GameType): Flow<Int> =
        gameScoreDao.observeBestScore(gameType.name)

    override fun observeRecentScores(limit: Int): Flow<List<GameScore>> =
        gameScoreDao.observeRecent(limit).map { rows ->
            rows.map { row ->
                GameScore(
                    gameType = GameType.fromKey(row.gameType),
                    score = row.score,
                    starsEarned = row.starsEarned,
                    playedAt = DateCodec.decode(row.playedAt) ?: LocalDate.now(),
                )
            }
        }

    override suspend fun clearAll() {
        lessonProgressDao.clear()
        gameScoreDao.clear()
    }

    // ------------------------------------------------------------------- stats

    override fun observeStats(): Flow<UserStats> =
        userStatsDao.observe().map { it?.toDomain() ?: UserStats.EMPTY }

    override fun observeBadges(): Flow<List<Badge>> =
        badgeDao.observeAll().map { rows -> rows.mapNotNull(::toDomain) }

    /**
     * Joins "how many lessons exist" (the in-APK catalog) with "how many are done"
     * (Room). This is a business rule, not a SQL query - the content side has no
     * table - so it lives here where both are available.
     *
     * Every module is always present in the list, even with zero progress, so the
     * dashboard's four bars never flicker in and out as the child progresses.
     */
    override fun observeModuleProgress(): Flow<List<ModuleProgress>> =
        lessonProgressDao.observeAll().map { rows ->
            val byModule = rows.groupBy { it.moduleType }
            ModuleType.entries.map { module ->
                val rowsForModule = byModule[module.name].orEmpty()
                val total = contentRepository.totalItems(module)
                ModuleProgress(
                    moduleType = module,
                    totalItems = total,
                    completedItems = rowsForModule.count { it.isCompleted },
                    totalStars = rowsForModule.sumOf { it.starsEarned },
                    maxStars = total * LessonProgress.MAX_STARS,
                )
            }
        }

    override fun observeDashboard(): Flow<DashboardSnapshot> = combine(
        observeStats(),
        observeModuleProgress(),
        observeBadges(),
        observeRecentScores(RECENT_SCORE_LIMIT),
    ) { stats, modules, badges, scores ->
        DashboardSnapshot(
            stats = stats,
            modules = modules,
            badges = badges,
            recentScores = scores,
            totalItems = modules.sumOf { it.totalItems },
        )
    }

    override fun observeWeeklyActivity(today: LocalDate): Flow<WeeklyActivity> = combine(
        lessonProgressDao.observeAll(),
        gameScoreDao.observeSince(DateCodec.encode(today.minusDays((WEEK_DAYS - 1).toLong()))),
    ) { lessons, games ->
        bucketWeekly(
            lessonDates = lessons.map { DateCodec.decode(it.lastAttemptDate) },
            gameDates = games.mapNotNull { DateCodec.decode(it.playedAt) },
            today = today,
        )
    }

    override suspend fun touchActivity(today: LocalDate): UserStats {
        userStatsDao.ensureRow()
        val current = userStatsDao.get() ?: UserStatsEntity()
        val result = calculateStreak(
            previousStreak = current.currentStreak,
            longestStreak = current.longestStreak,
            lastActiveEpochDay = DateCodec.decode(current.lastActiveDate)?.toEpochDay(),
            todayEpochDay = today.toEpochDay(),
        )
        userStatsDao.updateStreak(result.currentStreak, DateCodec.encode(today))
        return userStatsDao.get()?.toDomain() ?: UserStats.EMPTY
    }

    override suspend fun addStars(stars: Int) {
        if (stars == 0) return
        userStatsDao.addStarsEnsured(stars)
    }

    override suspend fun addCoins(coins: Int) {
        if (coins == 0) return
        userStatsDao.addCoinsEnsured(coins)
    }

    override suspend fun noteGamePlayed() = userStatsDao.incrementGamesPlayedEnsured()

    override suspend fun unlockBadges(keys: List<BadgeKey>, today: LocalDate): List<BadgeKey> {
        if (keys.isEmpty()) return emptyList()
        val existing = badgeDao.allKeys().toSet()
        // Filter in Kotlin as well as relying on `INSERT OR IGNORE` so the return
        // value is precisely the set of *newly* unlocked badges, which is what
        // drives the celebration animation.
        val fresh = keys.filter { it.key !in existing }
        if (fresh.isNotEmpty()) {
            val encoded = DateCodec.encode(today)
            badgeDao.insertAll(fresh.map { BadgeEntity(it.key, encoded) })
        }
        return fresh
    }

    override suspend fun resetAll() {
        userStatsDao.clear()
        badgeDao.clear()
    }

    // ------------------------------------------------------------------ mappers

    private fun toDomain(entity: com.freedu.kidslearn.data.local.entity.LessonProgressEntity): LessonProgress? {
        val module = ModuleType.entries.firstOrNull { it.name == entity.moduleType } ?: return null
        return LessonProgress(
            moduleType = module,
            itemId = entity.itemId,
            isCompleted = entity.isCompleted,
            starsEarned = entity.starsEarned,
            lastAttemptDate = DateCodec.decode(entity.lastAttemptDate),
        )
    }

    /**
     * An unrecognised badge key is dropped rather than throwing.
     *
     * A key can be unknown if the user downgraded, or if a row was written by a
     * newer build. Parsing it to the enum would crash the whole badge screen, so
     * the unknown key is simply not shown.
     */
    private fun toDomain(entity: BadgeEntity): Badge? {
        val key = BadgeKey.entries.firstOrNull { it.key == entity.badgeKey } ?: return null
        return Badge(badgeKey = key, unlockedDate = DateCodec.decode(entity.unlockedDate) ?: LocalDate.now())
    }

    private fun UserStatsEntity.toDomain() = UserStats(
        totalStars = totalStars,
        totalCoins = totalCoins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastActiveDate = DateCodec.decode(lastActiveDate),
        lessonsCompleted = lessonsCompleted,
        gamesPlayed = gamesPlayed,
    )

    companion object {
        const val RECENT_SCORE_LIMIT = 10
        const val WEEK_DAYS = 7
    }
}

/**
 * Buckets lesson-attempt and game dates into the 7 days ending [today].
 *
 * Pure so it is unit-testable without Room: rows with a null or out-of-window
 * date are ignored, and every day in the window is present even when empty.
 */
internal fun bucketWeekly(
    lessonDates: List<LocalDate?>,
    gameDates: List<LocalDate>,
    today: LocalDate,
    windowDays: Int = 7,
): WeeklyActivity {
    val start = today.minusDays((windowDays - 1).toLong())
    val lessonsByDay = lessonDates.filterNotNull()
        .filter { it in start..today }
        .groupingBy { it }
        .eachCount()
    val gamesByDay = gameDates
        .filter { it in start..today }
        .groupingBy { it }
        .eachCount()
    return WeeklyActivity(
        (0 until windowDays).map { offset ->
            val date = start.plusDays(offset.toLong())
            DayActivity(
                date = date,
                lessons = lessonsByDay[date] ?: 0,
                games = gamesByDay[date] ?: 0,
            )
        },
    )
}
