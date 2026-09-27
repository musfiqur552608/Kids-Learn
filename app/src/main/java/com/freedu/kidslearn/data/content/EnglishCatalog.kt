package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.LetterCategory
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.ModuleType

/**
 * The English A-Z catalog.
 *
 * Ships in the APK rather than being fetched, so a first launch on a plane still
 * works. Each entry is one tap target: the letter, its sound, a word it starts
 * and an emoji standing in for the picture.
 *
 * The `[visual]` emoji is a deliberate stand-in for licensed artwork. Swapping in
 * real illustrations is a one-line change per entry in a future release - see
 * `LessonItem.visual`.
 */
internal object EnglishCatalog {

    val letters: List<LetterItem> = listOf(
        letter("A", "a", "ay", "Apple", "আপেল", "🍎"),
        letter("B", "b", "bee", "Ball", "বল", "⚽"),
        letter("C", "c", "see", "Cat", "বিড়াল", "🐱"),
        letter("D", "d", "dee", "Dog", "কুকুর", "🐶"),
        letter("E", "e", "ee", "Egg", "ডিম", "🥚"),
        letter("F", "f", "eff", "Fish", "মাছ", "🐟"),
        letter("G", "g", "gee", "Goat", "ছাগল", "🐐"),
        letter("H", "h", "aitch", "Hat", "টুপি", "🎩"),
        letter("I", "i", "eye", "Ice cream", "আইসক্রিম", "🍦"),
        letter("J", "j", "jay", "Juice", "জুস", "🧃"),
        letter("K", "k", "kay", "Kite", "পতঙ্গ", "🪁"),
        letter("L", "l", "el", "Lion", "সিংহ", "🦁"),
        letter("M", "m", "em", "Moon", "চাঁদ", "🌙"),
        letter("N", "n", "en", "Nest", "আঁশ", "🪺"),
        letter("O", "o", "oh", "Orange", "কমলা", "🍊"),
        letter("P", "p", "pee", "Parrot", "তোতা", "🦜"),
        letter("Q", "q", "cue", "Queen", "রানি", "👑"),
        letter("R", "r", "ar", "Rabbit", "খরগোশ", "🐰"),
        letter("S", "s", "ess", "Sun", "সূর্য", "☀️"),
        letter("T", "t", "tee", "Tree", "গাছ", "🌳"),
        letter("U", "u", "you", "Umbrella", "ছাতা", "☂️"),
        letter("V", "v", "vee", "Van", "ভ্যান", "🚐"),
        letter("W", "w", "double u", "Watch", "ঘড়ি", "⌚"),
        letter("X", "x", "ex", "Xylophone", "জাইলোফোন", "🎹"),
        letter("Y", "y", "why", "Yo-yo", "ইয়ো-ইয়ো", "🪀"),
        letter("Z", "z", "zee", "Zebra", "জেবরা", "🦓"),
    )

    private fun letter(
        upper: String,
        lower: String,
        sound: String,
        word: String,
        meaning: String,
        visual: String,
    ) = LetterItem(
        id = "${ModuleType.ENGLISH.name}_$upper",
        moduleType = ModuleType.ENGLISH,
        letter = upper,
        secondary = lower,
        transliteration = sound,
        // English teaching convention is alphabetical, so no vowel/consonant split.
        category = LetterCategory.NONE,
        exampleWord = word,
        exampleMeaning = meaning,
        visual = visual,
    )
}
