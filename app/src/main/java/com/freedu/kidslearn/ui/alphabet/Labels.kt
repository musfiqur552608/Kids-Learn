package com.freedu.kidslearn.ui.alphabet

import androidx.annotation.StringRes
import com.freedu.kidslearn.R
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UiLanguage

/**
 * Maps domain enums to presentation resources.
 *
 * ## Why this lives in the UI layer
 * `R.string.*` is an Android resource id, and the domain layer deliberately has no
 * Android dependency so it can be unit-tested on a plain JVM. Keeping the mapping
 * here is what lets `ModuleType` stay a pure enum while the UI still renders
 * localised, resource-backed labels.
 */
@StringRes
fun ModuleType.labelRes(): Int = when (this) {
    ModuleType.ENGLISH -> R.string.module_english
    ModuleType.BANGLA -> R.string.module_bangla
    ModuleType.ARABIC -> R.string.module_arabic
    ModuleType.MATHS -> R.string.module_maths
}

/** Emoji, not a resource: `@StringRes` would be a type error in waiting. */
fun ModuleType.emoji(): String = when (this) {
    ModuleType.ENGLISH -> "🔤"
    ModuleType.BANGLA -> "🅱️"
    ModuleType.ARABIC -> "🕌"
    ModuleType.MATHS -> "🔢"
}

/** The emoji shown on the Home tile. Not a string resource: it is an icon. */
fun ModuleType.tileEmoji(): String = emoji()

/** Section heading for a letter category, in the app's UI language. */
@StringRes
fun categoryLabelRes(category: com.freedu.kidslearn.domain.model.LetterCategory): Int? =
    when (category) {
        com.freedu.kidslearn.domain.model.LetterCategory.VOWEL -> R.string.alphabet_vowels
        com.freedu.kidslearn.domain.model.LetterCategory.CONSONANT -> R.string.alphabet_consonants
        com.freedu.kidslearn.domain.model.LetterCategory.NONE -> null
    }

/** Maps a game enum to its name and subtitle. */
@StringRes
fun com.freedu.kidslearn.domain.model.GameType.titleRes(): Int = when (this) {
    com.freedu.kidslearn.domain.model.GameType.MEMORY_MATCH -> R.string.game_memory_match
    com.freedu.kidslearn.domain.model.GameType.FIND_THE_CORRECT_ONE -> R.string.game_find_correct
    com.freedu.kidslearn.domain.model.GameType.TIMED_QUIZ -> R.string.game_timed_quiz
}

@StringRes
fun com.freedu.kidslearn.domain.model.GameType.subtitleRes(): Int = when (this) {
    com.freedu.kidslearn.domain.model.GameType.MEMORY_MATCH -> R.string.game_memory_match_subtitle
    com.freedu.kidslearn.domain.model.GameType.FIND_THE_CORRECT_ONE -> R.string.game_find_correct_subtitle
    com.freedu.kidslearn.domain.model.GameType.TIMED_QUIZ -> R.string.game_timed_quiz_subtitle
}

@StringRes
fun com.freedu.kidslearn.domain.model.BadgeKey.titleRes(): Int = when (this) {
    com.freedu.kidslearn.domain.model.BadgeKey.FIRST_STEP -> R.string.badge_first_step
    com.freedu.kidslearn.domain.model.BadgeKey.FIRST_PERFECT -> R.string.badge_first_perfect
    com.freedu.kidslearn.domain.model.BadgeKey.ENGLISH_STAR -> R.string.badge_english_star
    com.freedu.kidslearn.domain.model.BadgeKey.BANGLA_STAR -> R.string.badge_bangla_star
    com.freedu.kidslearn.domain.model.BadgeKey.ARABIC_STAR -> R.string.badge_arabic_star
    com.freedu.kidslearn.domain.model.BadgeKey.MATHS_STAR -> R.string.badge_maths_star
    com.freedu.kidslearn.domain.model.BadgeKey.COLLECTOR_10 -> R.string.badge_collector_10
    com.freedu.kidslearn.domain.model.BadgeKey.COLLECTOR_25 -> R.string.badge_collector_25
    com.freedu.kidslearn.domain.model.BadgeKey.GAME_CHAMPION -> R.string.badge_game_champion
    com.freedu.kidslearn.domain.model.BadgeKey.STREAK_3 -> R.string.badge_streak_3
    com.freedu.kidslearn.domain.model.BadgeKey.STREAK_7 -> R.string.badge_streak_7
}

fun com.freedu.kidslearn.domain.model.BadgeKey.emoji(): String = when (this) {
    com.freedu.kidslearn.domain.model.BadgeKey.FIRST_STEP -> "👣"
    com.freedu.kidslearn.domain.model.BadgeKey.FIRST_PERFECT -> "🎯"
    com.freedu.kidslearn.domain.model.BadgeKey.ENGLISH_STAR -> "🔤"
    com.freedu.kidslearn.domain.model.BadgeKey.BANGLA_STAR -> "🅱️"
    com.freedu.kidslearn.domain.model.BadgeKey.ARABIC_STAR -> "🕌"
    com.freedu.kidslearn.domain.model.BadgeKey.MATHS_STAR -> "🔢"
    com.freedu.kidslearn.domain.model.BadgeKey.COLLECTOR_10 -> "⭐"
    com.freedu.kidslearn.domain.model.BadgeKey.COLLECTOR_25 -> "🏆"
    com.freedu.kidslearn.domain.model.BadgeKey.GAME_CHAMPION -> "🎮"
    com.freedu.kidslearn.domain.model.BadgeKey.STREAK_3 -> "🔥"
    com.freedu.kidslearn.domain.model.BadgeKey.STREAK_7 -> "🌟"
}

/** Locale tag for a module's pronunciation, mirroring `QuizFactory.ttsLocale`. */
fun ModuleType.speechLocale(): String = FeedbackPlayer.Locales.forModule(this)

fun UiLanguage.speechLocale(): String = FeedbackPlayer.Locales.forLanguage(this)
