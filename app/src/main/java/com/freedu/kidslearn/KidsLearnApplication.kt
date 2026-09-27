package com.freedu.kidslearn

import android.app.Application
import com.freedu.kidslearn.core.audio.SoundEffect
import com.freedu.kidslearn.core.audio.SoundEffectPlayer
import com.freedu.kidslearn.di.ApplicationScope
import com.freedu.kidslearn.domain.usecase.TouchActivityUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Application entry point.
 *
 * ## What runs here, and why here
 * [TouchActivityUseCase] updates the daily streak. It belongs at the Application
 * level rather than on the Home screen because:
 *
 *  * A cold start may restore straight into a lesson (Android restores the back
 *    stack), in which case a Home-only hook would miss the day entirely.
 *  * It must happen even if the child never navigates past the splash screen.
 *
 * ## Why it is fire-and-forget
 * The work is two indexed SQL statements. It runs on [applicationScope] rather
 * than blocking `onCreate`, and any failure is swallowed - a streak that is one
 * day late is a cosmetic bug, and crashing on launch to be correct about a streak
 * is not an acceptable trade for a children's app.
 */
@HiltAndroidApp
class KidsLearnApplication : Application() {

    @Inject
    lateinit var touchActivity: TouchActivityUseCase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var soundEffectPlayer: SoundEffectPlayer

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            runCatching { touchActivity(LocalDate.now()) }
                .onFailure {
                    // Logged rather than surfaced: nothing actionable for the family,
                    // and the streak self-heals on the next launch.
                    android.util.Log.w(TAG, "Could not update the daily streak", it)
                }
        }

        // A short page-turn on cold start gives the app an audible identity
        // instead of starting in silence.
        soundEffectPlayer.play(SoundEffect.PAGE_TURN)
    }

    private companion object {
        const val TAG = "KidsLearnApplication"
    }
}
