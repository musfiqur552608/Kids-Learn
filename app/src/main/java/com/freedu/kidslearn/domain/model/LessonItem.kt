package com.freedu.kidslearn.domain.model

/**
 * Anything a child can complete and be awarded stars for.
 *
 * Sealed so the UI can exhaustively branch (`when`) over "letter" vs "number"
 * and get a compile error if a third kind of lesson is ever added.
 */
sealed interface LessonItem {
    /** Stable id persisted in Room, e.g. `ENGLISH_A` or `MATHS_COUNT_07`. */
    val id: String

    /**
     * Emoji used as the "picture" for the lesson.
     *
     * Deliberately emoji rather than bitmap drawables: the app must be 100% offline
     * and install-light, and the system emoji font is already present on every
     * device. Swapping in real artwork later only requires changing this field.
     */
    val visual: String
}

/**
 * A single letter of an alphabet module.
 *
 * @param letter the primary glyph shown large on the card
 * @param secondary the companion glyph (lowercase for English; empty otherwise)
 * @param transliteration latin romanisation, so a child can sound out a
 *   Bangla/Arabic letter even before they can read the script
 * @param category vowels and consonants are presented as separate sub-sections
 *   for Bangla (স্বরবর্ণ / ব্যঞ্জনবর্ণ) and Arabic
 * @param exampleWord the word the letter introduces
 * @param exampleMeaning the same word in the language the app UI is using
 */
data class LetterItem(
    override val id: String,
    val moduleType: ModuleType,
    val letter: String,
    val secondary: String,
    val transliteration: String,
    val category: LetterCategory,
    val exampleWord: String,
    val exampleMeaning: String,
    override val visual: String,
) : LessonItem

/**
 * A counting / number-recognition lesson for the maths module.
 *
 * @param number the value being taught (1..20)
 * @param visualEmoji the object that is repeated [number] times on the card
 * @param word the number spelled out, for the audio prompt
 */
data class CountingItem(
    override val id: String,
    val number: Int,
    val visualEmoji: String,
    val word: String,
    override val visual: String,
) : LessonItem {
    /** The emoji repeated [number] times, for the "count the objects" card. */
    val repeatedVisual: String
        get() = visualEmoji.repeat(number)
}

/** A shape/colour lesson - the bonus maths sub-module. */
data class ShapeItem(
    override val id: String,
    val shapeName: String,
    val colourName: String,
    override val visual: String,
) : LessonItem

/** Sub-sections of an alphabet module. */
enum class LetterCategory {
    VOWEL,
    CONSONANT,
    NONE,
}
