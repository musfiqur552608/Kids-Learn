package com.freedu.kidslearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only history of game play-throughs.
 *
 * Indexed on `(game_type, score)` because the hottest query in the app is "what is
 * my best score for this game?", which runs on every game finish. Keeping history
 * rather than overwriting a single best-score row is what lets the parent zone
 * show recent play, which parents genuinely ask about.
 */
@Entity(
    tableName = "game_score",
    indices = [
        Index(value = ["game_type", "score"], name = "index_game_score_type_score"),
        Index(value = ["played_at"], name = "index_game_score_played_at"),
    ],
)
data class GameScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "game_type")
    val gameType: String,

    @ColumnInfo(name = "score")
    val score: Int,

    @ColumnInfo(name = "stars_earned")
    val starsEarned: Int = 0,

    /** ISO-8601 local date, `yyyy-MM-dd`. */
    @ColumnInfo(name = "played_at")
    val playedAt: String,
)
