package com.freedu.kidslearn.core.audio

import com.freedu.kidslearn.di.ApplicationScope
import com.freedu.kidslearn.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the raw system-TTS engine (as opposed to the bundled-first facade). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SystemTts

/**
 * The app's voice: bundled narration clips first, system TTS as fallback.
 *
 * This is what makes Listen work identically on every device. A Bangla lesson
 * plays its pre-generated `bn_lNN.mp3` whether or not the phone ships a
 * Bangla TTS voice - no download, no permission, no network. Lines with no
 * bundled clip (dynamic English prompts such as "3 plus 2", badge names)
 * delegate to [AndroidSpeechPlayer], which speaks them with the system engine
 * (en-US is present on virtually every device) or, if even that is missing,
 * plays its gentle chime fallback.
 *
 * Mute behaviour mirrors the other players: muted until the first settings
 * emission arrives, so no narration can leak out before a parent's preference
 * is known.
 */
@Singleton
class BundledSpeechPlayer @Inject constructor(
    private val manifest: NarrationManifest,
    private val clips: NarrationClipPlayer,
    @param:SystemTts private val systemTts: SpeechPlayer,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : SpeechPlayer {

    private val isMuted = AtomicBoolean(true)

    init {
        applicationScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                isMuted.set(!settings.soundEnabled)
            }
        }
    }

    override fun speak(text: String, localeTag: String, flush: Boolean) {
        if (isMuted.get() || text.isBlank()) return
        if (flush) runCatching { clips.stop() }
        val file = runCatching { manifest.fileFor(text, localeTag) }.getOrNull()
        if (file != null && runCatching { clips.play("$ASSET_DIR/$file") }.getOrDefault(false)) {
            return
        }
        systemTts.speak(text, localeTag, flush)
    }

    private companion object {
        const val ASSET_DIR = "narration"
    }
}
