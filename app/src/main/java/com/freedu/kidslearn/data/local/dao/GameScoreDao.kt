package com.freedu.kidslearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freedu.kidslearn.data.local.entity.GameScoreEntity
import kotlinx.coroutines.flow.Flow

/** Game play history. */
@Dao
interface GameScoreDao {

    @Query("SELECT * FROM game_score ORDER BY played_at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<GameScoreEntity>>

    /**
     * Everything played on or after [earliest] (`yyyy-MM-dd`). ISO dates order
     * lexicographically, so a plain string comparison is a date comparison.
     */
    @Query("SELECT * FROM game_score WHERE played_at >= :earliest ORDER BY played_at ASC")
    fun observeSince(earliest: String): Flow<List<GameScoreEntity>>

    /**
     * `COALESCE` matters: on a brand-new install the table is empty, and a bare
     * `MAX(score)` would emit `null` into a `Flow<Int>`, crashing the collector.
     */
    @Query("SELECT COALESCE(MAX(score), 0) FROM game_score WHERE game_type = :gameType")
    fun observeBestScore(gameType: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM game_score WHERE game_type = :gameType")
    fun observePlayCount(gameType: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(score: GameScoreEntity): Long

    @Query("DELETE FROM game_score")
    suspend fun clear()
}
