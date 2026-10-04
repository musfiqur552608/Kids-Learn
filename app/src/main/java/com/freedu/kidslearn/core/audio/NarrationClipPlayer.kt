package com.freedu.kidslearn.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays one bundled narration clip from APK assets.
 *
 * A separate seam from [SoundEffectPlayer] on purpose: effects are short,
 * often-overlapping UI blips suited to `SoundPool`, while narration lines are
 * seconds-long speech where overlap would be mud - each new line stops the
 * previous one. Returns success booleans (rather than throwing) so the caller
 * can fall back to system TTS when a clip is missing or unplayable.
 */
interface NarrationClipPlayer {
    /** Starts [assetPath] (relative to assets/, e.g. `"narration/bn_l01.mp3"`). */
    fun play(assetPath: String): Boolean

    /** Stops any in-progress clip. Safe to call when idle. */
    fun stop()
}

/**
 * `MediaPlayer`-backed clips, cached to internal storage on first use.
 *
 * The copy-through-`cacheDir` step is what makes playback immune to APK
 * compression settings: `AssetFileDescriptor` playback fails for compressed
 * assets on some devices, while a plain file path always works. If the system
 * ever clears the cache, the clip is simply re-copied on next tap.
 */
@Singleton
class MediaPlayerClipPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NarrationClipPlayer {

    private var current: MediaPlayer? = null

    @Synchronized
    override fun play(assetPath: String): Boolean {
        stop()
        return runCatching {
            val dst = File(context.cacheDir, assetPath).also { it.parentFile?.mkdirs() }
            if (!dst.exists()) {
                context.assets.open(assetPath).use { input ->
                    dst.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val player = MediaPlayer().apply {
                setDataSource(dst.absolutePath)
                setOnCompletionListener {
                    synchronized(this@MediaPlayerClipPlayer) {
                        runCatching { it.release() }
                        if (current === it) current = null
                    }
                }
                prepare()
                start()
            }
            current = player
            true
        }.onFailure {
            Log.w(TAG, "Could not play bundled clip $assetPath", it)
        }.getOrDefault(false)
    }

    @Synchronized
    override fun stop() {
        runCatching {
            current?.stop()
            current?.release()
        }
        current = null
    }

    private companion object {
        const val TAG = "NarrationClipPlayer"
    }
}
