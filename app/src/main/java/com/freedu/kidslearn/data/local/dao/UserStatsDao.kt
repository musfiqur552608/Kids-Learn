package com.freedu.kidslearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.freedu.kidslearn.data.local.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats WHERE id = :id LIMIT 1")
    fun observe(id: Int = UserStatsEntity.SINGLETON_ID): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = UserStatsEntity.SINGLETON_ID): UserStatsEntity?

    /**
     * Atomic increments.
     *
     * These deliberately do `col = col + :n` inside SQL instead of reading the row
     * in Kotlin and writing it back. Although a child can realistically only earn
     * one thing at a time, a quiz completion touches stars, coins *and* streak
     * concurrently from different coroutines, and a read-modify-write in Kotlin
     * would silently lose whichever increment lost the race.
     *
     * The `WHERE id = ...` clause doubles as the "insert if missing" mechanism:
     * we always try the update first, and only `ensureRow()` when nothing matched.
     */
    @Query(
        """
        UPDATE user_stats
        SET total_stars = total_stars + :stars
        WHERE id = :id
        """,
    )
    suspend fun addStars(stars: Int, id: Int = UserStatsEntity.SINGLETON_ID)

    @Query(
        """
        UPDATE user_stats
        SET total_coins = total_coins + :coins
        WHERE id = :id
        """,
    )
    suspend fun addCoins(coins: Int, id: Int = UserStatsEntity.SINGLETON_ID)

    @Query(
        """
        UPDATE user_stats
        SET games_played = games_played + 1
        WHERE id = :id
        """,
    )
    suspend fun incrementGamesPlayed(id: Int = UserStatsEntity.SINGLETON_ID)

    /** Bumps `lessons_completed` only for lessons that were not already done. */
    @Query(
        """
        UPDATE user_stats
        SET lessons_completed = lessons_completed + :delta
        WHERE id = :id
        """,
    )
    suspend fun addLessonsCompleted(delta: Int, id: Int = UserStatsEntity.SINGLETON_ID)

    @Query(
        """
        UPDATE user_stats
        SET
            current_streak = :currentStreak,
            longest_streak = MAX(longest_streak, :currentStreak),
            last_active_date = :today
        WHERE id = :id
        """,
    )
    suspend fun updateStreak(
        currentStreak: Int,
        today: String,
        id: Int = UserStatsEntity.SINGLETON_ID,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stats: UserStatsEntity)

    /** Idempotent: safe to call before every mutation. */
    @Transaction
    suspend fun ensureRow(id: Int = UserStatsEntity.SINGLETON_ID) {
        if (get(id) == null) insertIfAbsent(UserStatsEntity(id = id))
    }

    @Transaction
    suspend fun addStarsEnsured(stars: Int, id: Int = UserStatsEntity.SINGLETON_ID) {
        ensureRow(id)
        addStars(stars, id)
    }

    @Transaction
    suspend fun addCoinsEnsured(coins: Int, id: Int = UserStatsEntity.SINGLETON_ID) {
        ensureRow(id)
        addCoins(coins, id)
    }

    @Transaction
    suspend fun incrementGamesPlayedEnsured(id: Int = UserStatsEntity.SINGLETON_ID) {
        ensureRow(id)
        incrementGamesPlayed(id)
    }

    @Query("DELETE FROM user_stats")
    suspend fun clear()
}
