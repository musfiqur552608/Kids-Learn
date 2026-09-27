package com.freedu.kidslearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per (module, lesson).
 *
 * ## Schema decisions
 * * `itemId` is a **string**, not an int. Lesson ids are stable textual keys
 *   (`ENGLISH_A`, `MATHS_COUNT_07`); using the string means content can grow or be
 *   reordered in a future release without invalidating stored rows.
 * * Composite uniqueness on `(module_type, item_id)` makes the write an upsert, so
 *   a child who repeats a lesson overwrites their record instead of accumulating
 *   duplicates. The index also serves the per-module dashboard query, which is by
 *   far the most frequent read in the app.
 * * `module_type` is stored via a `TypeConverter` as the enum **name**, never the
 *   ordinal - see `DatabaseConverters`.
 * * `last_attempt_date` is an ISO-8601 *string* (`yyyy-MM-dd`) rather than epoch
 *   millis. Streaks are a calendar-day concept, and storing the local date means
 *   a timezone change cannot retroactively rewrite a child's history.
 */
@Entity(
    tableName = "lesson_progress",
    indices = [
        Index(
            value = ["module_type", "item_id"],
            name = "index_lesson_progress_module_item",
            unique = true,
        ),
        Index(value = ["module_type"], name = "index_lesson_progress_module"),
    ],
)
data class LessonProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "module_type")
    val moduleType: String,

    @ColumnInfo(name = "item_id")
    val itemId: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "stars_earned")
    val starsEarned: Int = 0,

    @ColumnInfo(name = "last_attempt_date")
    val lastAttemptDate: String? = null,
)
