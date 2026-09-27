package com.freedu.kidslearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freedu.kidslearn.data.local.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

/** Unlocked badges. */
@Dao
interface BadgeDao {

    @Query("SELECT * FROM badge ORDER BY unlocked_date DESC, badge_key ASC")
    fun observeAll(): Flow<List<BadgeEntity>>

    @Query("SELECT badge_key FROM badge")
    suspend fun allKeys(): List<String>

    @Query("SELECT COUNT(*) FROM badge")
    fun observeCount(): Flow<Int>

    /**
     * `INSERT OR IGNORE` is what makes unlocking idempotent: the primary key is the
     * badge key, so a second attempt is a no-op instead of a constraint violation.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(badges: List<BadgeEntity>)

    @Query("DELETE FROM badge")
    suspend fun clear()
}
