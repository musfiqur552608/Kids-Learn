package com.freedu.kidslearn.di

import android.content.Context
import androidx.room.Room
import com.freedu.kidslearn.data.local.KidsDatabase
import com.freedu.kidslearn.data.local.dao.BadgeDao
import com.freedu.kidslearn.data.local.dao.GameScoreDao
import com.freedu.kidslearn.data.local.dao.LessonProgressDao
import com.freedu.kidslearn.data.local.dao.UserStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the local database and its DAOs.
 *
 * ## Why no `@Binds` module for the DAOs
 * Room's generated DAO implementations already satisfy the DAO *interfaces*, so
 * the abstract class returned by [KidsDatabase] is the only thing that needs
 * binding. Hilt will happily construct it for us.
 *
 * ## Why everything is `@Singleton`
 * A second database instance would mean a second connection pool, a second
 * connection and a second set of WAL files for the same file. Room itself throws
 * on that, and the app is single-process, so process-scoped is correct.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): KidsDatabase = KidsDatabase.getInstance(context)

    @Provides
    fun provideLessonProgressDao(database: KidsDatabase): LessonProgressDao =
        database.lessonProgressDao()

    @Provides
    fun provideGameScoreDao(database: KidsDatabase): GameScoreDao = database.gameScoreDao()

    @Provides
    fun provideBadgeDao(database: KidsDatabase): BadgeDao = database.badgeDao()

    @Provides
    fun provideUserStatsDao(database: KidsDatabase): UserStatsDao = database.userStatsDao()
}
