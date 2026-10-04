package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.LetterCategory
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.ModuleType

/**
 * Arabic (حروف / huroof) catalog - the 28 letters, isolated forms only.
 *
 * ## Isolated forms only
 * Arabic has contextual forms (initial / medial / final / isolated). Teaching all
 * four to a 3-8 year old is cognitively overwhelming and, for a first pass,
 * actively harmful: the same letter looks like four different letters. The
 * [LessonItem.letter] field therefore always holds the isolated (nukta) form.
 *
 * ## Vowel vs consonant split
 * Unlike Bangla, Arabic teaches its short vowels as letters in their own right, so
 * the three vowel *letters* - alif, waw and ya - are categorised as
 * [LetterCategory.VOWEL] and the remaining 25 as [LetterCategory.CONSONANT].
 *
 * The hamza carriers (أ إ ء) and the long vowels (aa, ii, uu) are deliberately
 * omitted. They are taught later, and introducing them here would make the catalog
 * 31 letters while claiming to be an alphabet of 28.
 *
 * ## Transliteration
 * The emphatic consonants take capitals (Sa Da Ta Za) so ص/س, ض/د, ط/ت and ظ/ز
 * stay distinct without apostrophe hacks, and alif is "aa" (a long vowel) so it
 * never collides with ayn's "a". The labels are display hints for the parent,
 * never speech input - the engine always receives the Arabic script itself.
 *
 * ## Rendering
 * Requires the bundled Noto Naskh Arabic font. The module UI additionally forces
 * `LayoutDirection.Rtl`; `exampleWord` carries optional short vowels (tashkeel) so
 * a child can read the word rather than guess it.
 */
internal object ArabicCatalog {

    val vowels: List<LetterItem> = listOf(
        vowel("ا", "aa", "أَسَد", "Lion", "🦁"),
        vowel("و", "u", "وَرْدَة", "Rose", "🌹"),
        vowel("ي", "i", "يَد", "Hand", "✋"),
    )

    val consonants: List<LetterItem> = listOf(
        consonant("ب", "b", "بَطَّة", "Duck", "🦆"),
        consonant("ت", "t", "تُفَّاحَة", "Apple", "🍎"),
        consonant("ث", "th", "ثَعْلَب", "Fox", "🦊"),
        consonant("ج", "j", "جَمَل", "Camel", "🐫"),
        consonant("ح", "h", "حُوت", "Whale", "🐳"),
        consonant("خ", "kh", "خَرُوف", "Sheep", "🐑"),
        consonant("د", "d", "دُبّ", "Bear", "🐻"),
        consonant("ذ", "dh", "ذُرَة", "Corn", "🌽"),
        consonant("ر", "r", "رَجُل", "Man", "👨"),
        consonant("ز", "z", "زَرَافَة", "Giraffe", "🦒"),
        consonant("س", "s", "سَمَك", "Fish", "🐟"),
        consonant("ش", "sh", "شَمْس", "Sun", "☀️"),
        consonant("ص", "Sa", "صَقْر", "Falcon", "🦅"),
        consonant("ض", "Da", "ضِفْدَع", "Frog", "🐸"),
        consonant("ط", "Ta", "طَائِرَة", "Airplane", "✈️"),
        consonant("ظ", "Za", "ظَرْف", "Envelope", "✉️"),
        consonant("ع", "a", "عَيْن", "Eye", "👁️"),
        consonant("غ", "gh", "غَيْمَة", "Cloud", "☁️"),
        consonant("ف", "f", "فِيل", "Elephant", "🐘"),
        consonant("ق", "q", "قِطَّة", "Cat", "🐈"),
        consonant("ك", "k", "كِتَاب", "Book", "📗"),
        consonant("ل", "l", "لَيْمُون", "Lemon", "🍋"),
        consonant("م", "m", "مَوْز", "Banana", "🍌"),
        consonant("ن", "n", "نَجْم", "Star", "⭐"),
        consonant("ه", "h", "هِلَال", "Crescent", "🌙"),
    )

    val letters: List<LetterItem> = vowels + consonants

    private fun vowel(glyph: String, sound: String, word: String, meaning: String, visual: String) =
        LetterItem(
            id = "${ModuleType.ARABIC.name}_V_$glyph",
            moduleType = ModuleType.ARABIC,
            letter = glyph,
            secondary = "",
            transliteration = sound,
            category = LetterCategory.VOWEL,
            exampleWord = word,
            exampleMeaning = meaning,
            visual = visual,
        )

    private fun consonant(glyph: String, sound: String, word: String, meaning: String, visual: String) =
        LetterItem(
            id = "${ModuleType.ARABIC.name}_C_$glyph",
            moduleType = ModuleType.ARABIC,
            letter = glyph,
            secondary = "",
            transliteration = sound,
            category = LetterCategory.CONSONANT,
            exampleWord = word,
            exampleMeaning = meaning,
            visual = visual,
        )
}
