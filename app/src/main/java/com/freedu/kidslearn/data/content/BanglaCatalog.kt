package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.LetterCategory
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.ModuleType

/**
 * Bangla (বাংলা) catalog, split into স্বরবর্ণ (vowels) and ব্যঞ্জনবর্ণ (consonants).
 *
 * ## Counting note
 * 11 vowels + 35 consonants. The commonly quoted "39 consonants" counts ঁ, ং, ঃ
 * and ৎ, which are *diacritics*, not independent letters - they are omitted here
 * because a 3-8 year old cannot yet be expected to read them standalone.
 *
 * ## Example words
 * Placeholder set chosen from the words a Bangla-medium preschool primer
 * introduces first. `exampleMeaning` carries a second rendering so the card is
 * still useful when the app UI is toggled to English.
 *
 * Rendering requires the bundled Noto Sans Bengali font (see `ui/theme/Type.kt`);
 * the platform default font has no Bangla coverage.
 */
internal object BanglaCatalog {

    val vowels: List<LetterItem> = listOf(
        vowel("অ", "ô", "আম", "Mango", "🍎"),
        vowel("আ", "ā", "আঙুল", "Finger", "👆"),
        vowel("ই", "i", "ইলু", "Watermelon", "🍉"),
        vowel("ঈ", "ī", "ঈগা", "Fly", "🪰"),
        vowel("উ", "u", "উচু", "High", "⬆️"),
        vowel("ঊ", "ū", "ঊঁটা", "Tadpole", "🐸"),
        vowel("ঋ", "ri", "ঋতু", "Season", "🍂"),
        vowel("এ", "e", "এক", "One", "1️⃣"),
        vowel("ঐ", "oi", "ঐকী", "Together", "🤝"),
        vowel("ও", "o", "ওলা", "Pot", "🏺"),
        vowel("ঔ", "ou", "ঔষধ", "Medicine", "💊"),
    )

    val consonants: List<LetterItem> = listOf(
        consonant("ক", "ko", "কমলা", "Orange", "🍊"),
        consonant("খ", "kho", "খেলা", "Ball", "⚽"),
        consonant("গ", "go", "গাছ", "Tree", "🌳"),
        consonant("ঘ", "gho", "ঘড়ি", "Clock", "⏰"),
        consonant("ঙ", "ng", "রঙ", "Colour", "🎨"),
        consonant("চ", "cho", "চাবি", "Key", "🔑"),
        consonant("ছ", "chho", "ছাতা", "Umbrella", "☂️"),
        consonant("জ", "jo", "জাম", "Jam", "🍯"),
        consonant("ঝ", "jho", "ঝাল", "Curry", "🍛"),
        consonant("ঞ", "nyo", "অঞ্জন", "Flame", "🔥"),
        consonant("ট", "tto", "টমেটো", "Tomato", "🍅"),
        consonant("ঠ", "tho", "ঠেলা", "Cart", "🛒"),
        consonant("ড", "ddo", "ডাব", "Coconut", "🥥"),
        consonant("ঢ", "ddho", "ঢাকা", "Dhaka", "🏙️"),
        consonant("ণ", "nno", "কণ্ঠ", "Voice", "🎤"),
        consonant("ত", "to", "তাল", "Palm", "🌴"),
        consonant("থ", "tho2", "থালা", "Plate", "🍽️"),
        consonant("দ", "do", "দোকান", "Shop", "🏪"),
        consonant("ধ", "dho", "ধনুক", "Bow", "🏹"),
        consonant("ন", "no", "নৌকা", "Boat", "⛵"),
        consonant("প", "po", "পাখি", "Bird", "🐦"),
        consonant("ফ", "pho", "ফুল", "Flower", "🌸"),
        consonant("ব", "bo", "বই", "Book", "📕"),
        consonant("ভ", "bho", "ভাতা", "Rice", "🍚"),
        consonant("ম", "mo", "মিষ্টি", "Sweets", "🍬"),
        consonant("য", "jo2", "জয়ন্ত", "Celebration", "🎉"),
        consonant("র", "ro", "রবি", "Sun", "☀️"),
        consonant("ল", "lo", "লাল", "Red", "❤️"),
        consonant("শ", "sho", "শিশু", "Child", "👶"),
        consonant("ষ", "kho2", "ষড়", "Six", "6️⃣"),
        consonant("স", "so", "সমুদ্র", "Sea", "🌊"),
        consonant("হ", "ho", "হাত", "Hand", "✋"),
        consonant("ড়", "rro", "পাড়া", "Riverbank", "🌾"),
        consonant("ঢ়", "rrho", "ঢেঁড়া", "Owl", "🦉"),
        consonant("য়", "yo", "রায়", "King", "👑"),
    )

    /** Teaching order: every vowel, then every consonant. */
    val letters: List<LetterItem> = vowels + consonants

    private fun vowel(glyph: String, sound: String, word: String, meaning: String, visual: String) =
        LetterItem(
            id = "${ModuleType.BANGLA.name}_V_$glyph",
            moduleType = ModuleType.BANGLA,
            letter = glyph,
            // Bangla has no letter-case, so the "companion" slot stays empty and
            // the card renders a single large glyph instead of a pair.
            secondary = "",
            transliteration = sound,
            category = LetterCategory.VOWEL,
            exampleWord = word,
            exampleMeaning = meaning,
            visual = visual,
        )

    private fun consonant(glyph: String, sound: String, word: String, meaning: String, visual: String) =
        LetterItem(
            id = "${ModuleType.BANGLA.name}_C_$glyph",
            moduleType = ModuleType.BANGLA,
            letter = glyph,
            secondary = "",
            transliteration = sound,
            category = LetterCategory.CONSONANT,
            exampleWord = word,
            exampleMeaning = meaning,
            visual = visual,
        )
}
