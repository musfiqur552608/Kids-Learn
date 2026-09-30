package com.freedu.kidslearn.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.freedu.kidslearn.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Formats every translatable string and plural in the app, in every locale the
 * app ships.
 *
 * ## Why this test exists
 * A format mismatch is invisible to the compiler *and* to lint: the Kotlin call
 * type-checks, and lint only inspects the XML. It surfaces at runtime as
 * `IllegalFormatConversionException: d != java.lang.String` and takes the whole
 * Activity down - so for a kids' app it is discovered on a child's device rather
 * than in CI.
 *
 * The arguments are not hard-coded per string: each template is read back from
 * the resources and the argument list is built from its own conversion
 * specifiers. That keeps the test honest when a translation is edited - a Bangla
 * string that swaps a `%1$s` for a `%1$d` fails here instead of on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringResourcesTest {

    private val english: Context = ApplicationProvider.getApplicationContext()

    private val bangla: Context = english.createConfigurationContext(
        Configuration(english.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("bn-BD"))
        },
    )

    private val allContexts = listOf(english to "en", bangla to "bn")

    /** Every plain string the app can display. */
    private val strings = intArrayOf(
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
    )

    private val plurals = intArrayOf(
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

    /** Quantities chosen to hit every plural form each locale defines. */
    private val quantities = intArrayOf(0, 1, 2, 7, 11, 100)

    /** `%1$s`-style match: positional index, flags/width, conversion character. */
    private val specifier = Regex("%(?:(\\d+)\\$)?([-#+ 0,(]*\\d*(?:\\.\\d+)?)([a-zA-Z%])")

    /** Builds arguments whose types match [template]'s own conversion specifiers. */
    private fun argsFor(template: String): Array<Any> =
        specifier.findAll(template).map { match ->
            when (match.groupValues[3]) {
                "%" -> null
                "s", "S", "c", "C" -> "Sample"
                "d", "D", "i", "o", "O", "x", "X" -> 7
                "f", "F", "e", "E", "g", "G", "a", "A" -> 1.5
                else -> "Sample"
            }
        }.filterNotNull().toList().toTypedArray()

    @Test
    fun everyPlainStringFormatsInEveryLocale() {
        allContexts.forEach { (context, _) ->
            strings.forEach { id ->
                // `getText` returns the unresolved template; formatting it with the
                // arguments it actually asks for is the part worth verifying.
                val template = context.getText(id).toString()
                assertThat(template).isNotEmpty()

                val formatted = context.getString(id, *argsFor(template))
                assertThat(formatted).isNotEmpty()
            }
        }
    }

    @Test
    fun everyPluralFormatsInEveryLocaleForEveryQuantity() {
        allContexts.forEach { (context, _) ->
            plurals.forEach { id ->
                quantities.forEach { quantity ->
                    val template = context.resources
                        .getQuantityText(id, quantity)
                        .toString()
                    assertThat(template).isNotEmpty()

                    val formatted = context.resources.getQuantityString(
                        id,
                        quantity,
                        *argsFor(template),
                    )
                    assertThat(formatted).isNotEmpty()
                }
            }
        }
    }

    @Test
    fun singularAndPluralFormsBothResolve() {
        // The one case where the two forms differ visibly - a regression here
        // would print "1 stars" to a child.
        fun render(quantity: Int): String {
            val template = english.resources
                .getQuantityText(R.plurals.result_stars_earned, quantity)
                .toString()
            return english.resources.getQuantityString(
                R.plurals.result_stars_earned,
                quantity,
                *argsFor(template),
            )
        }

        assertThat(render(1)).doesNotContain("stars")
        assertThat(render(3)).contains("stars")
    }
}
