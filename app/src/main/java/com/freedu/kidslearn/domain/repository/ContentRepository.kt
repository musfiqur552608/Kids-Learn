package com.freedu.kidslearn.domain.repository

import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.CountingItem
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.LessonItem
import com.freedu.kidslearn.domain.model.LessonProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.domain.model.ShapeItem
import kotlinx.coroutines.flow.Flow

/**
 * Read-only access to the learning *content*.
 *
 * Content ships inside the APK (a compiled catalog in the data layer) rather than
 * being downloaded, so this is a synchronous lookup wrapped in an interface purely
 * so the domain does not depend on the catalog's storage format.
 */
interface ContentRepository {
    /** All letters of a module, in teaching order (vowels then consonants). */
    fun letters(moduleType: ModuleType): List<LetterItem>

    fun letter(moduleType: ModuleType, itemId: String): LetterItem?

    /** Counting lessons 1..20. */
    fun countingItems(): List<CountingItem>

    fun countingItem(number: Int): CountingItem?

    /** Shapes + colours sub-module. */
    fun shapeItems(): List<ShapeItem>

    /** Total number of completable items in a module - the dashboard denominator. */
    fun totalItems(moduleType: ModuleType): Int

    /**
     * Builds a quiz for [moduleType].
     *
     * @param itemIds restrict the quiz to these lessons (a detail screen asks
     *   about one letter); when empty a mixed quiz is generated.
     * @param count how many questions to generate.
     * @param seed makes generation deterministic, which is what lets the
     *   ViewModels and unit tests reproduce an exact paper.
     */
    fun buildQuiz(
        moduleType: ModuleType,
        itemIds: List<String> = emptyList(),
        count: Int = DEFAULT_QUIZ_LENGTH,
        seed: Long = 0L,
    ): List<QuizQuestion>

    /**
     * One-time check that every badge currently stored in Room is still known to
     * this build of the app. Called on startup so a downgrade cannot crash the
     * badge screen on an unknown key.
     */
    fun knownBadges(storedKeys: List<String>): List<Badge>

    companion object {
        const val DEFAULT_QUIZ_LENGTH = 5
    }
}

/** Write access to a child's learning history. */
interface ProgressRepository {
    fun observeLessonProgress(moduleType: ModuleType): Flow<List<LessonProgress>>

    fun observeAllLessonProgress(): Flow<List<LessonProgress>>

    /** Upserts a lesson row, keeping the child's best star count. */
    suspend fun recordLesson(
        moduleType: ModuleType,
        itemId: String,
        starsEarned: Int,
        today: java.time.LocalDate,
    )

    /** Records a game result. Returns true when it beat the previous best. */
    suspend fun recordGame(result: GameScore): Boolean

    fun observeBestScore(gameType: GameType): Flow<Int>

    fun observeRecentScores(limit: Int = 10): Flow<List<GameScore>>

    suspend fun clearAll()
}
