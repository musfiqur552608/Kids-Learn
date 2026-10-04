package com.freedu.kidslearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.freedu.kidslearn.data.local.entity.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * All reads return [Flow] so the UI recomposes the moment a lesson is completed -
 * there is no manual refresh anywhere in the app.
 */
@Dao
interface LessonProgressDao {

    @Query("SELECT * FROM lesson_progress ORDER BY module_type ASC, item_id ASC")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE module_type = :moduleType ORDER BY item_id ASC")
    fun observeByModule(moduleType: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE module_type = :moduleType AND item_id = :itemId LIMIT 1")
    suspend fun find(moduleType: String, itemId: String): LessonProgressEntity?

    @Query("SELECT * FROM lesson_progress WHERE module_type = :moduleType AND item_id = :itemId LIMIT 1")
    fun observeOne(moduleType: String, itemId: String): Flow<LessonProgressEntity?>

    /**
     * Writes a lesson result.
     *
     * The statement is written as a manual upsert rather than `@Upsert` because
     * `stars_earned` must be a **high-water mark**: a child who scored 3 stars
     * yesterday and only 1 today should keep their 3. Using
     * `MAX(stars_earned, excluded.stars_earned)` inside the same statement also
     * makes the read-modify-write race-free, which a `SELECT` then `UPDATE` in
     * Kotlin would not be.
     *
     * Completion is granted by *stars*, not by attempts: a row written with zero
     * stars (a quiz where nothing was answered correctly) records the attempt
     * date but leaves `is_completed` untouched. Marking a failed attempt
     * "learned" would move the home progress bars for zero achievement and hand
     * out the first-step badge for it.
     */
    @Query(
        """
        INSERT INTO lesson_progress (
            module_type, item_id, is_completed, stars_earned, last_attempt_date
        ) VALUES (
            :moduleType, :itemId, CASE WHEN :starsEarned > 0 THEN 1 ELSE 0 END, :starsEarned, :today
        )
        ON CONFLICT (module_type, item_id) DO UPDATE SET
            stars_earned = MAX(lesson_progress.stars_earned, excluded.stars_earned),
            is_completed = CASE WHEN excluded.stars_earned > 0 THEN 1 ELSE lesson_progress.is_completed END,
            last_attempt_date = excluded.last_attempt_date
        """,
    )
    suspend fun recordResult(
        moduleType: String,
        itemId: String,
        starsEarned: Int,
        today: String,
    )

    /** Bulk variant used when one quiz covers several lessons. */
    @Transaction
    suspend fun recordResults(
        moduleType: String,
        itemIds: List<String>,
        starsEarned: Int,
        today: String,
    ) {
        itemIds.forEach { recordResult(moduleType, it, starsEarned, today) }
    }

    @Query("SELECT COUNT(*) FROM lesson_progress WHERE module_type = :moduleType AND is_completed = 1")
    fun observeCompletedCount(moduleType: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(stars_earned), 0) FROM lesson_progress WHERE module_type = :moduleType")
    fun observeStarCount(moduleType: String): Flow<Int>

    @Query("DELETE FROM lesson_progress")
    suspend fun clear()
}
