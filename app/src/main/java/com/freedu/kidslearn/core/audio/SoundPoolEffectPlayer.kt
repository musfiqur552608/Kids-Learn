package com.freedu.kidslearn.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the bundled sound effects.
 *
 * @see SoundEffect for why `SoundPool` is the right tool here.
 */
interface SoundEffectPlayer {
    fun play(effect: SoundEffect)

    /** Releases the underlying native audio resources. */
    fun release()
}

/**
 * `SoundPool`-backed player that honours the "sound on/off" setting reactively.
 *
 * ## How the mute toggle works
 * Rather than checking a flag on every play, the player *subscribes* to
 * [SettingsRepository.settings] and calls [SoundPool.autoPause] / [autoResume].
 * That makes the toggle instant and, more importantly, makes it impossible to
 * forget the check in one of the several call sites.
 */
@Singleton
class SoundPoolEffectPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : SoundEffectPlayer {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /**
     * `SoundPool` returns 0 if loading failed. Keys are kept so a failed load
     * simply becomes a no-op play rather than a crash on a non-zero id.
     */
    private val soundIds: Map<SoundEffect, Int> = SoundEffect.entries.associateWith { effect ->
        runCatching { soundPool.load(context, effect.rawResId, 1) }
            .onFailure { Log.w(TAG, "Could not load ${effect.name} audio", it) }
            .getOrDefault(0)
    }

    private var isMuted: Boolean = true

    init {
        // Mute until the first settings emission arrives, so audio can never leak
        // out during the splash screen before a parent's preference is known.
        soundPool.autoPause()
        applicationScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                val muted = !settings.soundEnabled
                if (muted != isMuted) {
                    isMuted = muted
                    if (muted) soundPool.autoPause() else soundPool.autoResume()
                }
            }
        }
    }

    override fun play(effect: SoundEffect) {
        if (isMuted) return
        val id = soundIds[effect] ?: return
        if (id == 0) return
        soundPool.play(id, effect.volume, effect.volume, PRIORITY, 0, RATE_NORMAL)
    }

    override fun release() {
        soundPool.release()
    }

    private companion object {
        const val TAG = "SoundPoolEffectPlayer"

        /**
         * Four concurrent streams. Memory scales with this number, and on a 1 GB
         * phone three overlapping cues is already enough to sound busy rather
         * than cluttered.
         */
        const val MAX_STREAMS = 4

        const val PRIORITY = 1
        const val RATE_NORMAL = 1.0f
    }
}
