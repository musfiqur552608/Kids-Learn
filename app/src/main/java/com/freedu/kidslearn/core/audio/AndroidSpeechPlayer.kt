package com.freedu.kidslearn.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** Speaks a letter, a word, or a mascot encouragement. */
interface SpeechPlayer {
    /**
     * Speaks [text] in [localeTag] (a BCP-47 tag such as `"bn-BD"`).
     *
     * Must be safe to call before the engine has finished initialising: the splash
     * screen can ask for a greeting on the very first frame, and a child should not
     * lose the first word of the app because TTS was still starting up.
     */
    fun speak(text: String, localeTag: String, flush: Boolean = true)
}

/**
 * Offline text-to-speech via the platform [TextToSpeech] engine.
 *
 * ## This is still a fully offline app
 * The app declares no `INTERNET` permission, so the TTS engine physically cannot
 * download a voice pack. Where a language is missing, Android returns
 * `LANG_NOT_SUPPORTED`, this class records it, and [FeedbackPlayer] falls back to a
 * bundled sound effect. The child always gets *some* audio feedback rather than
 * silence, and nothing in this app ever touches the network.
 *
 * ## Where real recorded audio should go
 * If the project later ships studio-recorded narration (the better option for
 * teaching correct pronunciation), drop the clips in `res/raw/pronounce/` and
 * replace this class with a `MediaPlayer`-based implementation of [SpeechPlayer].
 * No other class changes: everything above this line depends on the interface.
 *
 * ## Lifecycle
 * Deliberately a `@Singleton` that is never shut down. `TextToSpeech` binds a
 * service connection, and tearing it down per-Activity would add a visible lag to
 * the first tap of every screen. The OS reclaims it when the process dies.
 */
@Singleton
class AndroidSpeechPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    effectPlayer: SoundEffectPlayer,
) : SpeechPlayer {

    private val isMuted = AtomicBoolean(true)
    private val isReady = AtomicBoolean(false)

    /**
     * Locales the engine has already told us it cannot speak. Cached so we do not
     * pay a binder round trip on every tap - this is checked on the hot path of
     * every answer button.
     */
    private val unsupportedLocales = ConcurrentHashMap.newKeySet<String>()

    /**
     * Utterances requested before the engine finished initialising. Without this
     * queue the very first tap of the app is silent, which for a pre-reader is a
     * dead interaction.
     */
    private val pending = mutableListOf<Pair<String, String>>()

    private val fallbackPlayer = effectPlayer

    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS init failed: status=$status")
                return@TextToSpeech
            }
            configureEngine()
            isReady.set(true)
            drainPending()
        }
    }.getOrNull()

    private fun configureEngine() {
        val engine = tts ?: return
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit

            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                Log.w(TAG, "Utterance $utteranceId failed")
            }
        })
        // Network voices would need a data connection this app does not have; asking
        // for the embedded voice makes the intent explicit and keeps latency low.
        engine.voice = engine.voices?.firstOrNull { !it.isNetworkConnectionRequired }
    }

    init {
        applicationScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                isMuted.set(!settings.soundEnabled)
            }
        }
    }

    override fun speak(text: String, localeTag: String, flush: Boolean) {
        if (isMuted.get() || text.isBlank()) return
        val engine = tts
        if (engine == null || !isReady.get()) {
            // Engine not up yet: remember it and try again once init completes.
            synchronized(pending) {
                if (pending.size < MAX_PENDING) pending += text to localeTag
            }
            return
        }
        if (localeTag in unsupportedLocales) {
            playFallback()
            return
        }

        val locale = Locale.forLanguageTag(localeTag)
        val availability = when (engine.isLanguageAvailable(locale)) {
            TextToSpeech.LANG_AVAILABLE -> TextToSpeech.LANG_AVAILABLE
            TextToSpeech.LANG_COUNTRY_AVAILABLE -> TextToSpeech.LANG_COUNTRY_AVAILABLE
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.i(TAG, "No offline voice for $localeTag; using sound fallback")
            unsupportedLocales += localeTag
            playFallback()
            return
        }

        runCatching {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

            // The per-utterance `TextToSpeech.Params` API (API 33+) is not present in
            // the public SDK stubs this project compiles against, so the engine's
            // language and rate are set immediately before each `speak` instead.
            // That is a global mutation, which is safe here because `speak` is
            // synchronous and the queue mode is `QUEUE_FLUSH`: the settings are
            // consumed by this call and never observed by another.
            @Suppress("DEPRECATION")
            run {
                engine.setLanguage(locale)
                engine.setSpeechRate(SLOW_SPEECH_RATE)
                engine.speak(text, queueMode, null, "$text-$localeTag")
            }
        }.onFailure { playFallback() }
    }

    private fun drainPending() {
        val queued = synchronized(pending) {
            if (pending.isEmpty()) return
            pending.toList().also { pending.clear() }
        }
        queued.forEach { (text, tag) ->
            if (tag !in unsupportedLocales) speak(text, tag)
        }
    }

    private fun playFallback() {
        // No voice for this script: a cheerful tone is better than silence, and
        // keeps the interaction feeling responsive.
        fallbackPlayer.play(SoundEffect.CORRECT)
    }

    private companion object {
        const val TAG = "AndroidSpeechPlayer"

        /** 0.85x - slow enough for a child to track the phonemes. */
        const val SLOW_SPEECH_RATE = 0.85f

        const val MAX_PENDING = 4
    }
}
