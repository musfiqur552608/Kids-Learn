package com.freedu.kidslearn.data.content

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the offline-only content promise: every lesson must be teachable from the
 * APK alone, with no network and no downloadable assets.
 *
 * A test like this is worth more than a comment, because the failure it catches -
 * a new catalog entry shipping with a blank example word or a missing picture -
 * is exactly the kind of thing that reaches production unnoticed.
 */
class ContentCatalogTest {

    @Test
    fun `the English catalog covers the whole alphabet`() {
        val letters = EnglishCatalog.letters.map { it.letter }

        assertThat(letters).containsExactlyElementsIn(('A'..'Z').map { it.toString() })
    }

    @Test
    fun `the Bangla catalog has the standard eleven vowels`() {
        assertThat(BanglaCatalog.vowels).hasSize(11)
        assertThat(BanglaCatalog.vowels.map { it.letter })
            .containsExactly("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ").inOrder()
    }

    @Test
    fun `the Bangla catalog is ordered vowels before consonants`() {
        // Vowels must all precede consonants, because the Bangla sub-sections are
        // taught in that order.
        val categories = BanglaCatalog.letters.map { it.category }

        val firstConsonant = categories.indexOfFirst {
            it == com.freedu.kidslearn.domain.model.LetterCategory.CONSONANT
        }

        // Every vowel index must come before the first consonant index.
        categories.forEachIndexed { index, category ->
            if (category == com.freedu.kidslearn.domain.model.LetterCategory.VOWEL) {
                assertThat(index).isLessThan(firstConsonant)
            }
        }
        assertThat(firstConsonant).isEqualTo(BanglaCatalog.vowels.size)
    }

    @Test
    fun `the Arabic catalog has exactly twenty eight letters`() {
        assertThat(ArabicCatalog.letters).hasSize(28)
    }

    @Test
    fun `Arabic letters are unique`() {
        val letters = ArabicCatalog.letters.map { it.letter }

        assertThat(letters.distinct()).hasSize(letters.size)
    }

    @Test
    fun `counting covers one to twenty with a distinct object for each`() {
        val numbers = MathsCatalog.counting.map { it.number }

        assertThat(numbers).containsExactlyElementsIn((1..20).toList())
        val emojis = MathsCatalog.counting.map { it.visualEmoji }
        assertThat(emojis.distinct()).hasSize(emojis.size)
    }

    @Test
    fun `the shapes catalog covers both colours and shapes`() {
        val colours = MathsCatalog.shapes.filter { it.id.contains("_COLOUR_") }
        val shapes = MathsCatalog.shapes.filter { it.id.contains("_SHAPE_") }

        assertThat(colours).isNotEmpty()
        assertThat(shapes).isNotEmpty()
    }

    @Test
    fun `transliterations carry no disambiguation hacks`() {
        // tho2 / kho2 / s' shipped: digit and apostrophe suffixes that were
        // invented to keep labels distinct and then displayed (and nearly
        // spoken) verbatim. Distinctness now comes from proper romanisation.
        val all = EnglishCatalog.letters + BanglaCatalog.letters + ArabicCatalog.letters

        all.forEach { item ->
            assertThat(item.transliteration).doesNotContain("'")
            assertThat(item.transliteration.contains(Regex("[0-9]"))).isFalse()
        }
    }

    @Test
    fun `visuals are unique within each alphabet module`() {
        // The quiz rejects same-emoji distractors per question; a duplicate in
        // the pool would silently shrink the variety instead of failing loudly.
        listOf(
            EnglishCatalog.letters,
            BanglaCatalog.letters,
            ArabicCatalog.letters,
        ).forEach { letters ->
            val visuals = letters.map { it.visual }
            assertThat(visuals.distinct()).hasSize(visuals.size)
        }
    }

    @Test
    fun `bangla example words match their pictures`() {
        // Regression lock for the pass that replaced mismatched pairs
        // (আম for অ which starts with আ, "ball" for খেলা, non-words like ঈগা/ঊঁটা,
        // যাদু which is standardly জাদু, ঠেলাগাড়ি shown as shopping cart,
        // লাল shown as heart, ঋতু abstract season, অঞ্জলি shown as prayer).
        val byLetter = BanglaCatalog.letters.associateBy { it.letter }
        val expected = mapOf(
            // letter to Triple(word, meaning, visual)
            "অ" to Triple("অজগর", "Python", "🐍"),
            "ই" to Triple("ইঁদুর", "Rat", "🐀"),
            "ঈ" to Triple("ঈগল", "Eagle", "🦅"),
            "উ" to Triple("উট", "Camel", "🐫"),
            "ঊ" to Triple("ঊষা", "Dawn", "🌅"),
            "ঋ" to Triple("ঋষি", "Sage", "🧙"),
            "ঐ" to Triple("ঐক্য", "Unity", "🤝"),
            "ও" to Triple("ওড়না", "Scarf", "🧣"),
            "খ" to Triple("খরগোশ", "Rabbit", "🐰"),
            "জ" to Triple("জাহাজ", "Ship", "🚢"),
            "ঝ" to Triple("ঝুড়ি", "Basket", "🧺"),
            "ঞ" to Triple("অঞ্জলি", "Offering", "👐"),
            "ঠ" to Triple("ঠোঁট", "Lip", "👄"),
            "ভ" to Triple("ভাত", "Rice", "🍚"),
            "য" to Triple("যান", "Vehicle", "🚗"),
            "ল" to Triple("লাল", "Red", "🔴"),
            "ষ" to Triple("ষাঁড়", "Bull", "🐂"),
            "ড়" to Triple("বাড়ি", "House", "🏠"),
            "ঢ়" to Triple("আষাঢ়", "Monsoon", "🌧️"),
            "য়" to Triple("ময়ূর", "Peacock", "🦚"),
        )

        expected.forEach { (letter, triple) ->
            val item = byLetter.getValue(letter)
            assertThat(item.exampleWord).isEqualTo(triple.first)
            assertThat(item.exampleMeaning).isEqualTo(triple.second)
            assertThat(item.visual).isEqualTo(triple.third)
        }
    }

    @Test
    fun `arabic and english corrected pairings`() {
        val arabic = ArabicCatalog.letters.associateBy { it.letter }
        assertThat(arabic.getValue("ظ").exampleWord).isEqualTo("ظَرْف")
        assertThat(arabic.getValue("ظ").visual).isEqualTo("✉️")
        assertThat(arabic.getValue("ق").exampleWord).isEqualTo("قِطَّة")
        // Regression lock: ر was dance verb, ل was dialect milk, غ was deer-for-gazelle.
        assertThat(arabic.getValue("ر").exampleWord).isEqualTo("رَجُل")
        assertThat(arabic.getValue("ر").visual).isEqualTo("👨")
        assertThat(arabic.getValue("ل").exampleWord).isEqualTo("لَيْمُون")
        assertThat(arabic.getValue("ل").visual).isEqualTo("🍋")
        assertThat(arabic.getValue("غ").exampleWord).isEqualTo("غَيْمَة")
        assertThat(arabic.getValue("غ").visual).isEqualTo("☁️")

        val english = EnglishCatalog.letters.associateBy { it.letter }
        assertThat(english.getValue("N").exampleMeaning).isEqualTo("বাসা")
        assertThat(english.getValue("K").exampleMeaning).isEqualTo("ঘুড়ি")
        assertThat(english.getValue("X").exampleWord).isEqualTo("Xmas tree")
        assertThat(english.getValue("X").visual).isEqualTo("🎄")
    }

    @Test
    fun `an unknown badge key is dropped rather than throwing`() {
        val stored = listOf("FIRST_STEP", "A_BADGE_FROM_THE_FUTURE", "STREAK_7")

        val known = ContentRepositoryImpl().knownBadges(stored)

        assertThat(known.map { it.badgeKey.name })
            .containsExactly("FIRST_STEP", "STREAK_7")
    }

    @Test
    fun `an empty stored badge list is handled`() {
        assertThat(ContentRepositoryImpl().knownBadges(emptyList())).isEmpty()
    }
}
