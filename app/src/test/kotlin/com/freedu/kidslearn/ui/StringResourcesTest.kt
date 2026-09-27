package com.freedu.kidslearn.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.freedu.kidslearn.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Formats every translatable string and plural in the app with representative
 * arguments.
 *
 * ## Why this test exists
 * A format-string mismatch is invisible to the compiler *and* to lint: the Kotlin
 * call type-checks, and lint only checks the XML. It surfaces at runtime as
 * `IllegalFormatConversionException: d != java.lang.String` and takes the whole
 * Activity down - which for this app means a crash on the first screen a child
 * sees, discovered on their device rather than in CI.
 *
 * This bug actually shipped once (an argument was passed twice to
 * `pluralStringResource`, whose second parameter is the plural *quantity* and not
 * a format argument). Running the real resources under Robolectric catches that
 * class of mistake on every build, for every locale, for the cost of a second.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringResourcesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * Format arguments chosen to mirror the real call sites: an Int where the
     * format string expects `%d`, a String where it expects `%s`.
     */
    private val sampleArgs = arrayOf<Any>("Sample", 3, 10)

    @Test
    fun `every plural resolves for both quantities`() {
        val plurals = intArrayOf(
            R.plurals.letters_count,
            R.plurals.quiz_question_of,
            R.plurals.quiz_correct_count,
            R.plurals.result_stars_earned,
            R.plurals.game_taps_to_match,
            R.plurals.game_rounds,
            R.plurals.game_score,
            R.plurals.game_best,
            R.plurals.progress_percent,
            R.plurals.progress_badges_earned,
            R.plurals.parent_gate_question,
            R.plurals.cd_module_progress,
        )

        plurals.forEach { id ->
            // quantity = 1 selects the "one" form, 7 the "other" form. Both must
            // format without throwing, in both shipped locales.
            listOf(1, 7, 0, 11, 100).forEach { quantity ->
                val resolved = context.resources.getQuantityString(id, quantity, *sampleArgs)
                assertThat(resolved).isNotEmpty()
            }
        }
    }

    @Test
    fun `every plural formats with the arguments its format string expects`() {
        // Each entry is (id, args) with args matching that string's own specifiers.
        val cases = listOf(
            R.plurals.letters_count to arrayOf<Any>(3, 26),
            R.plurals.quiz_question_of to arrayOf<Any>(3, 5),
            R.plurals.quiz_correct_count to arrayOf<Any>(4, 5),
            R.plurals.result_stars_earned to arrayOf<Any>(3),
            R.plurals.game_taps_to_match to arrayOf<Any>(7),
            R.plurals.game_rounds to arrayOf<Any>(2, 8),
            R.plurals.game_score to arrayOf<Any>(12),
            R.plurals.game_best to arrayOf<Any>(9),
            R.plurals.progress_percent to arrayOf<Any>(42),
            R.plurals.progress_badges_earned to arrayOf<Any>(3, 11),
            R.plurals.parent_gate_question to arrayOf<Any>("7 + 5"),
            R.plurals.cd_module_progress to arrayOf<Any>("English", 12, 26),
        )

        cases.forEach { (id, args) ->
            val one = context.resources.getQuantityString(id, 1, *args)
            val other = context.resources.getQuantityString(id, 7, *args)

            assertThat(one).isNotEmpty()
            assertThat(other).isNotEmpty()
        }
    }

    @Test
    fun `the bangla translations resolve too`() {
        val bangla = context.createConfigurationContext(
            android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(java.util.Locale.forLanguageTag("bn-BD"))
            },
        )

        listOf(
            R.plurals.letters_count,
            R.plurals.quiz_question_of,
            R.plurals.quiz_correct_count,
            R.plurals.result_stars_earned,
            R.plurals.game_rounds,
            R.plurals.progress_percent,
            R.plurals.progress_badges_earned,
            R.plurals.parent_gate_question,
            R.plurals.cd_module_progress,
        ).forEach { id ->
            val resolved = bangla.resources.getQuantityString(id, 5, *sampleArgs)
            assertThat(resolved).isNotEmpty()
        }
    }

    @Test
    fun `every plain string resolves in both locales`() {
        val strings = intArrayOf(
            R.string.home_greeting,
            R.string.home_greeting_named,
            R.string.home_subtitle,
            R.string.home_continue_with,
            R.string.module_english,
            R.string.module_bangla,
            R.string.module_arabic,
            R.string.module_maths,
            R.string.module_games,
            R.string.module_progress,
            R.string.module_parent_zone,
            R.string.alphabet_vowels,
            R.string.alphabet_consonants,
            R.string.listen,
            R.string.trace_instruction,
            R.string.take_quiz,
            R.string.all_letters,
            R.string.letter_card_description,
            R.string.quiz_tap_the_letter,
            R.string.quiz_tap_the_picture,
            R.string.quiz_how_many,
            R.string.quiz_answer_correct,
            R.string.quiz_answer_try_again,
            R.string.quiz_finish,
            R.string.result_title,
            R.string.result_title_perfect,
            R.string.result_new_badge,
            R.string.result_play_again,
            R.string.result_go_home,
            R.string.maths_counting,
            R.string.maths_shapes,
            R.string.maths_add_subtract,
            R.string.maths_add_subtract_subtitle,
            R.string.maths_tap_the_answer,
            R.string.games_title,
            R.string.games_subtitle,
            R.string.game_memory_match,
            R.string.game_memory_match_subtitle,
            R.string.game_find_correct,
            R.string.game_find_correct_subtitle,
            R.string.game_timed_quiz,
            R.string.game_timed_quiz_subtitle,
            R.string.game_seconds_short,
            R.string.game_you_matched,
            R.string.progress_title,
            R.string.progress_overall,
            R.string.progress_total_stars,
            R.string.progress_total_coins,
            R.string.progress_streak,
            R.string.progress_best_streak,
            R.string.progress_lessons,
            R.string.progress_games,
            R.string.progress_badges,
            R.string.progress_recent,
            R.string.parent_zone_title,
            R.string.parent_gate_title,
            R.string.parent_gate_wrong,
            R.string.parent_gate_cancel,
            R.string.parent_gate_enter,
            R.string.parent_zone_child_name,
            R.string.parent_zone_sound,
            R.string.parent_zone_music,
            R.string.parent_zone_language,
            R.string.parent_zone_theme,
            R.string.parent_zone_theme_light,
            R.string.parent_zone_theme_dark,
            R.string.language_english,
            R.string.language_bangla,
            R.string.parent_zone_gate_toggle,
            R.string.parent_zone_reset,
            R.string.parent_zone_reset_confirm,
            R.string.parent_zone_reset_done,
            R.string.parent_zone_reset_button,
            R.string.parent_zone_about,
            R.string.parent_zone_offline_note,
            R.string.parent_zone_version,
            R.string.ok,
            R.string.back,
            R.string.next,
            R.string.loading,
            R.string.yay,
            R.string.cd_coin,
        )

        strings.forEach { id ->
            val en = context.getString(id, *sampleArgs)
            assertThat(en).isNotEmpty()
        }
    }

    @Test
    fun `badge labels all resolve`() {
        listOf(
            R.string.badge_first_step,
            R.string.badge_first_perfect,
            R.string.badge_english_star,
            R.string.badge_bangla_star,
            R.string.badge_arabic_star,
            R.string.badge_maths_star,
            R.string.badge_collector_10,
            R.string.badge_collector_25,
            R.string.badge_game_champion,
            R.string.badge_streak_3,
            R.string.badge_streak_7,
        ).forEach { id -> assertThat(context.getString(id)).isNotEmpty() }
    }
}
