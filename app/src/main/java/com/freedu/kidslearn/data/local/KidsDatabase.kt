package com.freedu.kidslearn.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.freedu.kidslearn.data.local.dao.BadgeDao
import com.freedu.kidslearn.data.local.dao.GameScoreDao
import com.freedu.kidslearn.data.local.dao.LessonProgressDao
import com.freedu.kidslearn.data.local.dao.UserStatsDao
import com.freedu.kidslearn.data.local.entity.BadgeEntity
import com.freedu.kidslearn.data.local.entity.GameScoreEntity
import com.freedu.kidslearn.data.local.entity.LessonProgressEntity
import com.freedu.kidslearn.data.local.entity.UserStatsEntity

/**
 * The single local database. There is no remote data source by design - the app
 * declares no `INTERNET` permission, so this database *is* the app's entire
 * persistent state.
 *
 * ## Why entities store enum *names* as `String`
 * Rather than declaring `moduleType: ModuleType` and adding a `@TypeConverter`,
 * the entities take a `String`. That choice is deliberate:
 *
 *  * A `String` column needs no converter, so there is no conversion code to test.
 *  * The stored value is self-describing: `ENGLISH` is readable in a database dump,
 *    whereas an ordinal silently repoints every row at the wrong module the day
 *    someone inserts an enum constant in the middle.
 *  * The mapping to the enum happens exactly once, in
 *    `LocalProgressRepository.toDomain`, where an unknown value is a safe `null`.
 *
 * ## Schema versioning
 * `exportSchema = true` writes the schema to `app/schemas/` on every build so
 * `MigrationTestHelper` can verify upgrades automatically rather than relying on a
 * developer remembering to hand-write one.
 */
@Database(
    entities = [
        LessonProgressEntity::class,
        GameScoreEntity::class,
        BadgeEntity::class,
        UserStatsEntity::class,
    ],
    version = KidsDatabase.VERSION,
    exportSchema = true,
)
abstract class KidsDatabase : RoomDatabase() {

    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun gameScoreDao(): GameScoreDao
    abstract fun badgeDao(): BadgeDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        const val VERSION = 1
        const val NAME = "kids_learn.db"

        @Volatile
        private var instance: KidsDatabase? = null

        /**
         * Process-wide singleton.
         *
         * Note the absence of `allowMainThreadQueries()`: every DAO call is a
         * `suspend` function or returns a `Flow`, so Room dispatches the work to its
         * own executor and the main thread is never blocked. That is the single most
         * important rule for keeping this app smooth on cheap phones.
         */
        fun getInstance(context: Context): KidsDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): KidsDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                KidsDatabase::class.java,
                NAME,
            )
                .addMigrations(*MIGRATIONS)
                // Losing progress on a downgrade is recoverable; a crash loop is not.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /**
         * Migrations are additive only: each step adds columns/tables and never
         * rewrites or drops a child's history.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Pattern a future release would follow:
                    // db.execSQL(
                    //     "ALTER TABLE lesson_progress ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0",
                    // )
                }
            },
        )
    }
}
