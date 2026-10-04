package com.freedu.kidslearn.core.audio

import com.freedu.kidslearn.domain.model.ModuleType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Guards the lesson pronunciation phrasing per language.
 *
 * ## Why this test exists
 * `pronounceLesson` used to build every lesson line from the same English frame
 * ("খ. খ for খেলা") and hand it to the Bangla/Arabic voice. The engine then
 * pronounced the English word "for" as Bangla/Arabic, which is exactly the
 * "wrong sound" a parent reported. The frame is invisible in code review - it
 * looks like a harmless format string - so it is pinned here instead.
 */
class FeedbackPlayerTest {

    private val spoken = mutableListOf<Pair<String, String>>()

    private val speech = object : SpeechPlayer {
        override fun speak(text: String, localeTag: String, flush: Boolean) {
            spoken += text to localeTag
        }
    }

    private val effects = object : SoundEffectPlayer {
        val played = mutableListOf<SoundEffect>()
        override fun play(effect: SoundEffect) {
            played += effect
        }
        override fun release() = Unit
    }

    private lateinit var player: FeedbackPlayer

    @Before
    fun setUp() {
        spoken.clear()
        player = FeedbackPlayer(effects, speech)
    }

    @Test
    fun `english lessons name each case once`() {
        player.pronounceLesson(ModuleType.ENGLISH, "A", "a", "Apple")

        assertThat(spoken.single()).isEqualTo("Capital A, small a, A for Apple" to "en-US")
    }

    @Test
    fun `bangla lessons juxtapose letter and word with a bangla voice`() {
        player.pronounceLesson(ModuleType.BANGLA, "খ", "", "খরগোশ")

        val (text, locale) = spoken.single()
        assertThat(locale).isEqualTo("bn-BD")
        assertThat(text).isEqualTo("খ, খরগোশ")
        assertThat(text).doesNotContain("for")
    }

    @Test
    fun `arabic lessons juxtapose letter and word with an arabic voice`() {
        player.pronounceLesson(ModuleType.ARABIC, "ب", "", "بَطَّة")

        val (text, locale) = spoken.single()
        assertThat(locale).isEqualTo("ar-SA")
        assertThat(text).isEqualTo("ب, بَطَّة")
        assertThat(text).doesNotContain("for")
    }

    @Test
    fun `maths lessons juxtapose with an english voice`() {
        player.pronounceLesson(ModuleType.MATHS, "5", "", "Five")

        assertThat(spoken.single()).isEqualTo("5, Five" to "en-US")
    }

    @Test
    fun `praise rotation never says try again on a correct answer`() {
        repeat(10) {
            assertThat(FeedbackPlayer.Encouragement.random())
                .isNotEqualTo(FeedbackPlayer.Encouragement.TryAgain)
        }
    }
}
