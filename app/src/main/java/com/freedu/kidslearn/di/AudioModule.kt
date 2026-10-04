package com.freedu.kidslearn.di

import com.freedu.kidslearn.core.audio.AssetNarrationManifest
import com.freedu.kidslearn.core.audio.BundledSpeechPlayer
import com.freedu.kidslearn.core.audio.MediaPlayerClipPlayer
import com.freedu.kidslearn.core.audio.NarrationClipPlayer
import com.freedu.kidslearn.core.audio.NarrationManifest
import com.freedu.kidslearn.core.audio.SoundEffectPlayer
import com.freedu.kidslearn.core.audio.SoundPoolEffectPlayer
import com.freedu.kidslearn.core.audio.SpeechPlayer
import com.freedu.kidslearn.core.audio.AndroidSpeechPlayer
import com.freedu.kidslearn.core.audio.SystemTts
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the audio interfaces to their platform implementations.
 *
 * ## Why interfaces at all
 * Both players hold a native resource - a `SoundPool`'s audio buffers, a
 * `TextToSpeech` service connection - and both are time-sensitive. Keeping them
 * behind an interface means:
 *
 *  * a test can substitute a recording player and assert *which* cue fired, which
 *    is the only practical way to test positive-only feedback; and
 *  * narration voices are bundled clips (see `tools/generate_narration.py`) with
 *    system TTS behind them, so re-voicing is asset regeneration, not a refactor
 *    of every ViewModel.
 *
 * Both are `@Singleton` because the underlying native resource must be created
 * once per process: a second `SoundPool` would mean a second set of decoded audio
 * buffers for sounds that never change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindSoundEffectPlayer(impl: SoundPoolEffectPlayer): SoundEffectPlayer

    /**
     * The bundled-voice player is what the UI speaks through: pre-generated
     * clips for every lesson line, system TTS behind it for dynamic prompts.
     * (Swapping narration voices later is still a one-line change here.)
     */
    @Binds
    @Singleton
    abstract fun bindSpeechPlayer(impl: BundledSpeechPlayer): SpeechPlayer

    @Binds
    @Singleton
    abstract fun bindNarrationManifest(impl: AssetNarrationManifest): NarrationManifest

    @Binds
    @Singleton
    abstract fun bindNarrationClipPlayer(impl: MediaPlayerClipPlayer): NarrationClipPlayer

    companion object {
        /** The raw engine behind the bundled-first facade (see [SystemTts]). */
        @Provides
        @Singleton
        @SystemTts
        fun provideSystemTts(impl: AndroidSpeechPlayer): SpeechPlayer = impl
    }
}
