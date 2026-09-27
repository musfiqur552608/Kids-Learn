package com.freedu.kidslearn.di

import com.freedu.kidslearn.core.audio.AndroidSpeechPlayer
import com.freedu.kidslearn.core.audio.SoundEffectPlayer
import com.freedu.kidslearn.core.audio.SoundPoolEffectPlayer
import com.freedu.kidslearn.core.audio.SpeechPlayer
import dagger.Binds
import dagger.Module
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
 *  * swapping the TTS engine for bundled studio recordings later is a one-line
 *    change here rather than a refactor of every ViewModel.
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

    @Binds
    @Singleton
    abstract fun bindSpeechPlayer(impl: AndroidSpeechPlayer): SpeechPlayer
}
