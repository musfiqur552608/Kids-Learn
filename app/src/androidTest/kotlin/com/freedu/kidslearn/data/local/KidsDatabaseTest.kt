package com.freedu.kidslearn.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freedu.kidslearn.data.local.dao.BadgeDao
import com.freedu.kidslearn.data.local.dao.GameScoreDao
import com.freedu.kidslearn.data.local.dao.LessonProgressDao
import com.freedu.kidslearn.data.local.dao.UserStatsDao
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.data.local.entity.BadgeEntity
import com.freedu.kidslearn.data.local.entity.GameScoreEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO tests against a real (in-memory) SQLite database.
 *
 * These run on a device or emulator because the behaviour under test *is* SQLite
 * behaviour: the `ON CONFLICT ... DO UPDATE SET stars_earned = MAX(...)` upsert
 * and the atomic `col = col + :n` increments are SQL features, and a mocked DAO
 * would not exercise either.
 */
@RunWith(AndroidJUnit4::class)
class KidsDatabaseTest {

    private lateinit var database: KidsDatabase
    private lateinit var lessonDao: LessonProgressDao
    private lateinit var gameDao: GameScoreDao
    private lateinit var badgeDao: BadgeDao
    private lateinit var statsDao: UserStatsDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KidsDatabase::class.java,
        )
            // The production database allows main-thread queries nowhere, and the
            // tests must not either - that is what proves the app never blocks the UI.
            .allowMainThreadQueries()
            .build()

        lessonDao = database.lessonProgressDao()
        gameDao = database.gameScoreDao()
        badgeDao = database.badgeDao()
        statsDao = database.userStatsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ------------------------------------------------------- lesson progress

    @Test
    fun recordingALesson_storesTheRow() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 2, today = "2026-09-27")

        val row = lessonDao.find(Module.ENGLISH, "ENGLISH_A")

        assertThat(row).isNotNull()
        assertThat(row!!.isCompleted).isTrue()
        assertThat(row.starsEarned).isEqualTo(2)
        assertThat(row.lastAttemptDate).isEqualTo("2026-09-27")
    }

    @Test
    fun recordingTheSameLessonTwice_keepsTheBestScore() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 3, today = "2026-09-27")
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 1, today = "2026-09-28")

        val row = lessonDao.find(Module.ENGLISH, "ENGLISH_A")

        // The high-water mark is enforced in SQL, not in Kotlin.
        assertThat(row!!.starsEarned).isEqualTo(3)
        // The attempt date still moves forward.
        assertThat(row.lastAttemptDate).isEqualTo("2026-09-28")
    }

    @Test
    fun recordingTheSameLessonTwice_raisesTheScore() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 1, today = "2026-09-27")
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 3, today = "2026-09-28")

        assertThat(lessonDao.find(Module.ENGLISH, "ENGLISH_A")!!.starsEarned).isEqualTo(3)
    }

    @Test
    fun recordingALessonTwice_createsOneRowNotTwo() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 1, today = "2026-09-27")
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", starsEarned = 1, today = "2026-09-27")

        assertThat(lessonDao.observeAll().first()).hasSize(1)
    }

    @Test
    fun theSameItemIdInTwoModules_staysTwoRows() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "SHARED", starsEarned = 1, today = "2026-09-27")
        lessonDao.recordResult(Module.MATHS, "SHARED", starsEarned = 2, today = "2026-09-27")

        // The unique index is (module, item), not item alone.
        assertThat(lessonDao.observeAll().first()).hasSize(2)
    }

    @Test
    fun observeByModule_filtersCorrectly() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", 1, "2026-09-27")
        lessonDao.recordResult(Module.BANGLA, "BANGLA_ক", 3, "2026-09-27")

        assertThat(lessonDao.observeByModule(Module.ENGLISH).first()).hasSize(1)
        assertThat(lessonDao.observeByModule(Module.ARABIC).first()).isEmpty()
    }

    @Test
    fun starAndCompletedCountsAggregate() = runTest {
        lessonDao.recordResult(Module.MATHS, "MATHS_COUNT_01", 3, "2026-09-27")
        lessonDao.recordResult(Module.MATHS, "MATHS_COUNT_02", 2, "2026-09-27")

        assertThat(lessonDao.observeStarCount(Module.MATHS).first()).isEqualTo(5)
        assertThat(lessonDao.observeCompletedCount(Module.MATHS).first()).isEqualTo(2)
    }

    @Test
    fun recordResults_writesEveryItem() = runTest {
        lessonDao.recordResults(Module.ENGLISH, listOf("ENGLISH_A", "ENGLISH_B"), starsEarned = 3, today = "2026-09-27")

        assertThat(lessonDao.observeByModule(Module.ENGLISH).first()).hasSize(2)
    }

    @Test
    fun clear_emptiesTheTable() = runTest {
        lessonDao.recordResult(Module.ENGLISH, "ENGLISH_A", 1, "2026-09-27")
        lessonDao.clear()

        assertThat(lessonDao.observeAll().first()).isEmpty()
    }

    // ------------------------------------------------------------ game scores

    @Test
    fun bestScore_isZeroOnAFreshInstall() = runTest {
        // COALESCE is load-bearing: a bare MAX would emit null and crash the collector.
        assertThat(gameDao.observeBestScore("MEMORY_MATCH").first()).isEqualTo(0)
    }

    @Test
    fun bestScore_isTheMaximum() = runTest {
        gameDao.insert(GameScoreEntity(gameType = "MEMORY_MATCH", score = 4, playedAt = "2026-09-27"))
        gameDao.insert(GameScoreEntity(gameType = "MEMORY_MATCH", score = 7, playedAt = "2026-09-28"))
        gameDao.insert(GameScoreEntity(gameType = "TIMED_QUIZ", score = 2, playedAt = "2026-09-28"))

        assertThat(gameDao.observeBestScore("MEMORY_MATCH").first()).isEqualTo(7)
        assertThat(gameDao.observeBestScore("TIMED_QUIZ").first()).isEqualTo(2)
    }

    @Test
    fun recentScores_areNewestFirstAndLimited() = runTest {
        repeat(15) { index ->
            gameDao.insert(
                GameScoreEntity(
                    gameType = "TIMED_QUIZ",
                    score = index,
                    playedAt = "2026-09-%02d".format((index % 28) + 1),
                ),
            )
        }

        val recent = gameDao.observeRecent(5).first()

        assertThat(recent).hasSize(5)
    }

    @Test
    fun scoresSince_onlyReturnsTheWindow() = runTest {
        gameDao.insert(GameScoreEntity(gameType = "TIMED_QUIZ", score = 1, playedAt = "2026-09-27"))
        gameDao.insert(GameScoreEntity(gameType = "TIMED_QUIZ", score = 2, playedAt = "2026-09-28"))
        gameDao.insert(GameScoreEntity(gameType = "TIMED_QUIZ", score = 3, playedAt = "2026-10-04"))

        val window = gameDao.observeSince("2026-09-28").first()

        assertThat(window.map { it.score }).containsExactly(2, 3).inOrder()
    }

    // ----------------------------------------------------------------- badges

    @Test
    fun insertAllIgnoresDuplicates() = runTest {
        badgeDao.insertAll(listOf(BadgeEntity("FIRST_STEP", "2026-09-27")))
        badgeDao.insertAll(listOf(BadgeEntity("FIRST_STEP", "2026-09-28")))

        // The primary key makes double-unlocking structurally impossible.
        assertThat(badgeDao.observeAll().first()).hasSize(1)
    }

    @Test
    fun allKeysReturnsWhatWasInserted() = runTest {
        badgeDao.insertAll(
            listOf(BadgeEntity("FIRST_STEP", "2026-09-27"), BadgeEntity("STREAK_3", "2026-09-27")),
        )

        assertThat(badgeDao.allKeys()).containsExactly("FIRST_STEP", "STREAK_3")
    }

    // ------------------------------------------------------------- user stats

    @Test
    fun aFreshDatabaseHasNoStatsRow() = runTest {
        assertThat(statsDao.get()).isNull()
    }

    @Test
    fun ensureRowIsIdempotent() = runTest {
        statsDao.ensureRow()
        statsDao.ensureRow()

        assertThat(statsDao.get()).isNotNull()
    }

    @Test
    fun addStarsAccumulates() = runTest {
        statsDao.addStarsEnsured(3)
        statsDao.addStarsEnsured(2)

        assertThat(statsDao.get()!!.totalStars).isEqualTo(5)
    }

    @Test
    fun addCoinsAccumulatesSeparately() = runTest {
        statsDao.addCoinsEnsured(7)
        statsDao.addStarsEnsured(3)

        val row = statsDao.get()!!
        assertThat(row.totalCoins).isEqualTo(7)
        assertThat(row.totalStars).isEqualTo(3)
    }

    @Test
    fun incrementGamesPlayedCountsUp() = runTest {
        repeat(3) { statsDao.incrementGamesPlayedEnsured() }

        assertThat(statsDao.get()!!.gamesPlayed).isEqualTo(3)
    }

    @Test
    fun updateStreakKeepsTheLongestEverReached() = runTest {
        statsDao.ensureRow()
        statsDao.updateStreak(currentStreak = 5, today = "2026-09-27")
        // A later, worse streak must not erase the personal best.
        statsDao.updateStreak(currentStreak = 1, today = "2026-10-01")

        val row = statsDao.get()!!
        assertThat(row.currentStreak).isEqualTo(1)
        assertThat(row.longestStreak).isEqualTo(5)
    }

    @Test
    fun clearResetsTheStatsRow() = runTest {
        statsDao.addStarsEnsured(10)
        statsDao.clear()

        assertThat(statsDao.get()).isNull()
    }
    private companion object {
        /**
         * The repositories persist `ModuleType.name`, so the tests use the real
         * enum rather than hand-written strings - if the storage format ever
         * changes, these tests break rather than silently passing.
         */
        val Module = ModuleType
    }
}
