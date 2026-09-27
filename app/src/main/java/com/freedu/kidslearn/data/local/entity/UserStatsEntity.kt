package com.freedu.kidslearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lifetime totals. Exactly one row with `id = SINGLETON_ID`.
 *
 * ## Why not compute these with SQL aggregates?
 * `totalCoins` is awarded independently of lessons and games, and the streak needs
 * the *previous* value in order to compute the next one, so a plain `SUM()` cannot
 * express either. A single mutable row read in one query is both simpler and
 * faster on the low-end hardware this app targets.
 */
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    @ColumnInfo(name = "total_stars")
    val totalStars: Int = 0,

    @ColumnInfo(name = "total_coins")
    val totalCoins: Int = 0,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    /** ISO-8601 local date of the last time the app was opened. */
    @ColumnInfo(name = "last_active_date")
    val lastActiveDate: String? = null,

    @ColumnInfo(name = "lessons_completed")
    val lessonsCompleted: Int = 0,

    @ColumnInfo(name = "games_played")
    val gamesPlayed: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
