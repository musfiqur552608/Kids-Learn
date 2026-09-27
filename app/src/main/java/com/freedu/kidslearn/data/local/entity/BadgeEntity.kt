package com.freedu.kidslearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An unlocked badge.
 *
 * `badge_key` is the [com.freedu.kidslearn.domain.model.BadgeKey] enum name and is
 * the primary key. That single decision makes "unlock" an `INSERT OR IGNORE` and
 * makes double-unlocking structurally impossible rather than merely unlikely - no
 * amount of racing use cases can produce a duplicate row.
 */
@Entity(tableName = "badge")
data class BadgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "badge_key")
    val badgeKey: String,

    @ColumnInfo(name = "unlocked_date")
    val unlockedDate: String,
)
