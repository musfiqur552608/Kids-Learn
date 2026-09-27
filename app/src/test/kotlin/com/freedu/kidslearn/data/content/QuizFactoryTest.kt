package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The quiz generator is the app's main source of *invalid data* risk: a question
 * whose `correctIndex` points outside its options, or whose distractors duplicate
 * the answer, would show a child a quiz with no correct answer. These tests assert
 * the invariants that make that impossible.
 */
class QuizFactoryTest {

    private val factory = QuizFactory()
    private val repository = ContentRepositoryImpl(factory)

    @Test
    fun `every generated question has a correct index in range`() {
        ModuleType.entries.forEach { module ->
            repeat(50) { seed ->
                repository.buildQuiz(module, count = 5, seed = seed.toLong()).forEach { question ->
                    assertThat(question.correctIndex).isAtLeast(0)
                    assertThat(question.correctIndex).isLessThan(question.options.size)
                }
            }
        }
    }

    @Test
    fun `every question offers at least two distinct options`() {
        val questions = repository.buildQuiz(ModuleType.ENGLISH, count = 20, seed = 1L)

        questions.forEach { question ->
            assertThat(question.options).hasSize(com.freedu.kidslearn.data.content.QuizFactory.OPTION_COUNT)
            assertThat(question.options.map { it.id }.distinct()).hasSize(question.options.size)
        }
    }

    @Test
    fun `generation is deterministic for a given seed`() {
        val first = repository.buildQuiz(ModuleType.BANGLA, count = 5, seed = 99L)
        val second = repository.buildQuiz(ModuleType.BANGLA, count = 5, seed = 99L)

        assertThat(first.map { it.id }).isEqualTo(second.map { it.id })
        assertThat(first.map { it.correctIndex }).isEqualTo(second.map { it.correctIndex })
    }

    @Test
    fun `a single-lesson quiz only asks about that lesson`() {
        val questions = repository.buildQuiz(
            moduleType = ModuleType.ENGLISH,
            itemIds = listOf("ENGLISH_C"),
            count = 4,
            seed = 5L,
        )

        assertThat(questions).isNotEmpty()
        questions.forEach { question ->
            // The prompt or the answer must mention C; the distractors come from the
            // wider pool, which is the point of a distractor.
            val mentionsC = question.promptLabel == "C" ||
                question.options.any { it.label == "C" } ||
                question.promptVisual == repository.letter(ModuleType.ENGLISH, "ENGLISH_C")?.visual
            assertThat(mentionsC).isTrue()
        }
    }

    @Test
    fun `picture questions never offer two identical pictures`() {
        val questions = repository.buildQuiz(ModuleType.ENGLISH, count = 20, seed = 11L)
            .filter { it.kind == QuizKind.LETTER_TO_PICTURE }

        assertThat(questions).isNotEmpty()
        questions.forEach { question ->
            val visuals = question.options.map { it.visual }
            assertThat(visuals.distinct()).hasSize(visuals.size)
        }
    }

    @Test
    fun `maths questions never ask for a negative answer`() {
        val questions = repository.buildQuiz(ModuleType.MATHS, count = 40, seed = 3L)
            .filter { it.kind == QuizKind.SUBTRACTION }

        questions.forEach { question ->
            val answer = question.correctOption.label.toIntOrNull()
            assertThat(answer).isNotNull()
            assertThat(answer!!).isAtLeast(0)
        }
    }

    @Test
    fun `numeric maths answers stay inside the taught range`() {
        val questions = repository.buildQuiz(ModuleType.MATHS, count = 60, seed = 4L)
            .filter { it.kind in NUMERIC_KINDS }

        assertThat(questions).isNotEmpty()
        questions.forEach { question ->
            val answer = question.correctOption.label.toIntOrNull()
            assertThat(answer).isNotNull()
            assertThat(answer!!).isAtLeast(0)
            assertThat(answer).isAtMost(20)
        }
    }

    @Test
    fun `a module produces a mix of question kinds`() {
        val questions = repository.buildQuiz(ModuleType.MATHS, count = 20, seed = 8L)

        assertThat(questions.map { it.kind }.distinct().size).isGreaterThan(1)
    }

    @Test
    fun `an alphabet quiz never asks about another module's content`() {
        // The real risk is a mixed pool: a Bangla quiz sampling a ShapeItem would
        // ask a child to match the English word for a colour, which is unanswerable.
        val questions = repository.buildQuiz(ModuleType.BANGLA, count = 20, seed = 21L)
        val banglaVisuals = BanglaCatalog.letters.map { it.visual }.toSet()

        assertThat(questions).isNotEmpty()
        questions.forEach { question ->
            assertThat(question.kind).isAnyOf(QuizKind.PICTURE_TO_LETTER, QuizKind.LETTER_TO_PICTURE)
            // Spoken in Bangla, not English.
            assertThat(question.speakLocale).isEqualTo("bn-BD")
            // Every picture on offer is a picture from the Bangla catalog.
            question.options.forEach { option ->
                option.visual?.let { visual ->
                    assertThat(banglaVisuals).contains(visual)
                }
            }
        }
    }

    @Test
    fun `an alphabet quiz is spoken in the module's own language`() {
        val expected = mapOf(
            ModuleType.ENGLISH to "en-US",
            ModuleType.BANGLA to "bn-BD",
            ModuleType.ARABIC to "ar-SA",
        )

        expected.forEach { (module, locale) ->
            repository.buildQuiz(module, count = 5, seed = 3L).forEach { question ->
                assertThat(question.speakLocale).isEqualTo(locale)
            }
        }
    }

    @Test
    fun `the catalogue totals are what the dashboard denominator uses`() {
        assertThat(repository.totalItems(ModuleType.ENGLISH)).isEqualTo(26)
        assertThat(repository.totalItems(ModuleType.BANGLA)).isEqualTo(BanglaCatalog.letters.size)
        assertThat(repository.totalItems(ModuleType.ARABIC)).isEqualTo(ArabicCatalog.letters.size)
        assertThat(repository.totalItems(ModuleType.MATHS)).isEqualTo(20)
    }

    @Test
    fun `every content item has a unique id`() {
        val ids = EnglishCatalog.letters.map { it.id } +
            BanglaCatalog.letters.map { it.id } +
            ArabicCatalog.letters.map { it.id } +
            MathsCatalog.counting.map { it.id } +
            MathsCatalog.shapes.map { it.id }

        assertThat(ids.distinct()).hasSize(ids.size)
    }

    @Test
    fun `the maths quiz includes shape questions alongside arithmetic`() {
        val questions = repository.buildQuiz(ModuleType.MATHS, count = 30, seed = 77L)

        assertThat(questions.map { it.kind }).contains(QuizKind.SHAPE_COLOUR)
        assertThat(questions.map { it.kind }).containsAnyOf(
            QuizKind.COUNT_OBJECTS,
            QuizKind.ADDITION,
            QuizKind.SUBTRACTION,
        )
    }

    @Test
    fun `every letter has a word and a picture to teach with`() {
        (EnglishCatalog.letters + BanglaCatalog.letters + ArabicCatalog.letters).forEach { letter ->
            assertThat(letter.exampleWord).isNotEmpty()
            assertThat(letter.visual).isNotEmpty()
            assertThat(letter.transliteration).isNotEmpty()
        }
    }

    private companion object {
        /** The question kinds whose answer is a numeral. */
        val NUMERIC_KINDS = setOf(
            QuizKind.COUNT_OBJECTS,
            QuizKind.ADDITION,
            QuizKind.SUBTRACTION,
        )
    }
}
