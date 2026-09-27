package com.freedu.kidslearn.di

import com.freedu.kidslearn.data.content.ContentRepositoryImpl
import com.freedu.kidslearn.data.content.QuizFactory
import com.freedu.kidslearn.data.prefs.SettingsRepositoryImpl
import com.freedu.kidslearn.data.repository.LocalProgressRepository
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain interfaces to their data-layer implementations.
 *
 * Every use case and ViewModel depends on the *interface*, never the `Impl`. That
 * is what lets a unit test swap in an in-memory fake with a single `@TestInstallIn`
 * replacement module, and it means the dependency graph still points inwards
 * (data -> domain) rather than domain -> data.
 *
 * `ContentRepositoryImpl` and `SettingsRepositoryImpl` are `@Inject`-annotated and
 * therefore do not need a provider here; the factory below is the exception
 * because [QuizFactory] takes constructor *defaults* that Hilt cannot see through.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideQuizFactory(): QuizFactory = QuizFactory()

    @Provides
    @Singleton
    fun provideContentRepository(quizFactory: QuizFactory): ContentRepository =
        ContentRepositoryImpl(quizFactory)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository = impl

    /**
     * One implementation satisfies both interfaces.
     *
     * Two `@Provides` methods return the *same* `LocalProgressRepository` instance
     * because Hilt caches a provider's result for the component lifetime - so the
     * two bindings below do not create two objects, and both see the same Room
     * caches. Documented here because it is subtle and load-bearing: if the
     * implementation ever became `@Provides`-created per injection, the two
     * interfaces could silently disagree.
     */
    @Provides
    @Singleton
    fun provideProgressRepository(
        impl: LocalProgressRepository,
    ): ProgressRepository = impl

    @Provides
    @Singleton
    fun provideStatsRepository(
        impl: LocalProgressRepository,
    ): StatsRepository = impl
}
