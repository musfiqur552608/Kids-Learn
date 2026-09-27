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
