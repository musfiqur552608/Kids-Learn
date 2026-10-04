package com.freedu.kidslearn.core.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.freedu.kidslearn.data.content.ArabicCatalog
import com.freedu.kidslearn.data.content.BanglaCatalog
import com.freedu.kidslearn.data.content.EnglishCatalog
import com.freedu.kidslearn.data.content.MathsCatalog
import com.freedu.kidslearn.domain.model.lessonSpeechText
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the bundled-voice promise: every static Listen/quiz line the app can
 * speak must have a pre-generated clip in `assets/narration`.
 *
 * The expected lines are rebuilt here from the *real* catalogs with the *real*
 * phrasing rules (lesson card = quiz prompt - see QuizFactory), so a catalog
 * edit, a new letter, or a reworded prompt fails here until
 * `tools/generate_narration.py` is re-run. A missing clip is otherwise silent
 * at runtime: BundledSpeechPlayer falls back to system TTS, which on most
 * devices has no Bangla/Arabic voice - the "common ding instead of speech" bug.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundledNarrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val manifest: Map<String, String> by lazy {
        val json = context.assets.open("narration/manifest.json").bufferedReader().use {
            JSONObject(it.readText())
        }
        val entries = json.getJSONArray("entries")
        buildMap(entries.length()) {
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                put(
                    "${entry.getString("locale")}\n${entry.getString("text")}",
                    entry.getString("file"),
                )
            }
        }
    }

    /** Every (text, locale) the app speaks from a static catalog line. */
    private fun expectedLines(): List<Pair<String, String>> = buildList {
        // Lesson phrasing comes from the shared helper, never rebuilt here: a
        // rewording must fail this test until the clips are regenerated.
        EnglishCatalog.letters.forEach { item ->
            add(
                lessonSpeechText(item.moduleType, item.letter, item.secondary, item.exampleWord) to "en-US",
            )
            add(item.exampleWord to "en-US")
        }
        BanglaCatalog.letters.forEach { item ->
            add(
                lessonSpeechText(item.moduleType, item.letter, item.secondary, item.exampleWord) to "bn-BD",
            )
            add(item.exampleWord to "bn-BD")
        }
        ArabicCatalog.letters.forEach { item ->
            add(
                lessonSpeechText(item.moduleType, item.letter, item.secondary, item.exampleWord) to "ar-SA",
            )
            add(item.exampleWord to "ar-SA")
        }
        MathsCatalog.counting.forEach { add(it.word to "en-US") }
        MathsCatalog.shapes.forEach { add(it.shapeName to "en-US") }
        add("How many?" to "en-US")
    }

    @Test
    fun `every static lesson and quiz line has a bundled clip`() {
        val missing = expectedLines().filter { (text, locale) ->
            manifest["$locale\n$text"] == null
        }
        assertThat(missing).isEmpty()
    }

    @Test
    fun `every manifest clip ships in assets`() {
        val missingFiles = manifest.values.toSet().filter { file ->
            runCatching {
                context.assets.open("narration/$file").close()
            }.isFailure
        }
        assertThat(missingFiles).isEmpty()
    }
}
