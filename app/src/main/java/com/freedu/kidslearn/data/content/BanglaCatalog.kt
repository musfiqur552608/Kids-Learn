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
 * Curated from the words a Bangla-medium preschool primer introduces first -
 * every word must genuinely start with (or, for ঙ ঞ ণ য় ঢ় which never start a
 * word, genuinely contain) its letter, and the emoji must depict the word's
 * *meaning*, not just its first letter. `exampleMeaning` carries a second
 * rendering so the card is still useful when the app UI is toggled to English.
 *
 * ## Transliteration
 * Retroflexes take dotted letters (ṭ ḍ ṇ ṣ) so ট/ত, ঠ/থ, ড/দ, ঢ/ধ, ণ/ন and
 * ষ/শ/স stay distinct without digit hacks. The same dotted convention is used
 * by Bengali grammars, so a parent sounding the card out reads it correctly.
 *
 * Rendering requires the bundled Noto Sans Bengali font (see `ui/theme/Type.kt`);
 * the platform default font has no Bangla coverage.
 */
internal object BanglaCatalog {

    val vowels: List<LetterItem> = listOf(
        vowel("অ", "ô", "অজগর", "Python", "🐍"),
        vowel("আ", "ā", "আঙুল", "Finger", "👆"),
        vowel("ই", "i", "ইঁদুর", "Rat", "🐀"),
        vowel("ঈ", "ī", "ঈগল", "Eagle", "🦅"),
        vowel("উ", "u", "উট", "Camel", "🐫"),
        vowel("ঊ", "ū", "ঊষা", "Dawn", "🌅"),
        vowel("ঋ", "ri", "ঋষি", "Sage", "🧙"),
        vowel("এ", "e", "এক", "One", "1️⃣"),
        vowel("ঐ", "oi", "ঐক্য", "Unity", "🤝"),
        vowel("ও", "o", "ওড়না", "Scarf", "🧣"),
        vowel("ঔ", "ou", "ঔষধ", "Medicine", "💊"),
    )

    val consonants: List<LetterItem> = listOf(
        consonant("ক", "ko", "কমলা", "Orange", "🍊"),
        consonant("খ", "kho", "খরগোশ", "Rabbit", "🐰"),
        consonant("গ", "go", "গাছ", "Tree", "🌳"),
        consonant("ঘ", "gho", "ঘড়ি", "Clock", "⏰"),
        consonant("ঙ", "ng", "রঙ", "Colour", "🎨"),
        consonant("চ", "cho", "চাবি", "Key", "🔑"),
        consonant("ছ", "chho", "ছাতা", "Umbrella", "☂️"),
        consonant("জ", "jo", "জাহাজ", "Ship", "🚢"),
        consonant("ঝ", "jho", "ঝুড়ি", "Basket", "🧺"),
        consonant("ঞ", "nyo", "অঞ্জলি", "Offering", "👐"),
        consonant("ট", "ṭo", "টমেটো", "Tomato", "🍅"),
        consonant("ঠ", "ṭho", "ঠোঁট", "Lip", "👄"),
        consonant("ড", "ḍo", "ডাব", "Coconut", "🥥"),
        consonant("ঢ", "ḍho", "ঢাকা", "Dhaka", "🏙️"),
        consonant("ণ", "ṇo", "কণ্ঠ", "Voice", "🎤"),
        consonant("ত", "to", "তাল", "Palm", "🌴"),
        consonant("থ", "tho", "থালা", "Plate", "🍽️"),
        consonant("দ", "do", "দোকান", "Shop", "🏪"),
        consonant("ধ", "dho", "ধনুক", "Bow", "🏹"),
        consonant("ন", "no", "নৌকা", "Boat", "⛵"),
        consonant("প", "po", "পাখি", "Bird", "🐦"),
        consonant("ফ", "pho", "ফুল", "Flower", "🌸"),
        consonant("ব", "bo", "বই", "Book", "📕"),
        consonant("ভ", "bho", "ভাত", "Rice", "🍚"),
        consonant("ম", "mo", "মিষ্টি", "Sweets", "🍬"),
        consonant("য", "ja", "যান", "Vehicle", "🚗"),
        consonant("র", "ro", "রবি", "Sun", "☀️"),
        consonant("ল", "lo", "লাল", "Red", "🔴"),
        consonant("শ", "sho", "শিশু", "Child", "👶"),
        consonant("ষ", "ṣo", "ষাঁড়", "Bull", "🐂"),
        consonant("স", "so", "সমুদ্র", "Sea", "🌊"),
        consonant("হ", "ho", "হাত", "Hand", "✋"),
        consonant("ড়", "rro", "বাড়ি", "House", "🏠"),
        consonant("ঢ়", "rrho", "আষাঢ়", "Monsoon", "🌧️"),
        consonant("য়", "yo", "ময়ূর", "Peacock", "🦚"),
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
