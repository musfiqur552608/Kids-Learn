package com.freedu.kidslearn.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifiers for the two dispatchers this app cares about.
 *
 * Kept minimal on purpose. The app does very little CPU-bound work, so injecting a
 * single [ioDispatcher] and letting `Dispatchers.Main` (the Compose-safe default)
 * handle the rest avoids the usual over-engineering of four dispatchers with no
 * consumers.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Dispatcher and scope providers.
 *
 * ## Why the `ApplicationScope` qualifier exists
 * `TouchActivityUseCase` must run *outside* any screen's lifecycle, from
 * `Application.onCreate`. Without a scope tied to the process there would be no
 * correct place to launch that work, and the common workaround -
 * `GlobalScope.launch()` - is exactly the kind of fire-and-forget that leaves
 * work running after the last Activity is gone.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * `SupervisorJob` so one failed child coroutine (say, a corrupt stats row)
     * cannot cancel the whole process-scoped scope and silently stop future
     * streak updates.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
