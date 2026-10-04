package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.AnswerOption
import com.freedu.kidslearn.domain.model.CountingItem
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.LessonItem
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizKind
import com.freedu.kidslearn.domain.model.lessonSpeechText
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.domain.model.ShapeItem
import kotlin.random.Random

/**
 * Builds quiz papers from the offline catalogs.
 *
 * ## Determinism
 * Every method takes an explicit [seed] and uses [Random] rather than
 * `shuffled()`/`Random.default`. That is what makes a paper reproducible: a
 * Compose UI test can assert on the exact first question of a screen, and a bug
 * report can be replayed by re-entering the same seed.
 *
 * ## Distractor quality
 * A distractor that shares the correct answer's emoji (or is visually similar)
 * makes the question unfair to a child who is still learning. The generators
 * below explicitly reject candidates whose [LetterItem.visual] or
 * [CountingItem.visualEmoji] matches the answer, falling back to a plain random
 * pick only if the pool is too small to filter.
 */
class QuizFactory(
    private val english: List<LetterItem> = EnglishCatalog.letters,
    private val bangla: List<LetterItem> = BanglaCatalog.letters,
    private val arabic: List<LetterItem> = ArabicCatalog.letters,
    private val counting: List<CountingItem> = MathsCatalog.counting,
    private val shapes: List<ShapeItem> = MathsCatalog.shapes,
) {

    fun lettersFor(moduleType: ModuleType): List<LetterItem> = when (moduleType) {
        ModuleType.ENGLISH -> english
        ModuleType.BANGLA -> bangla
        ModuleType.ARABIC -> arabic
        ModuleType.MATHS -> emptyList()
    }

    fun buildQuiz(
        moduleType: ModuleType,
        itemIds: List<String>,
        count: Int,
        seed: Long,
    ): List<QuizQuestion> {
        val random = Random(seed)

        // The pool is scoped to the module *before* it is sampled from. Without
        // this, a mixed Bangla quiz could sample a ShapeItem and ask a child to
        // pick the English word for a colour - a question with no way to answer it.
        val alphabet = lettersFor(moduleType)
        val pool: List<LessonItem> = if (itemIds.isEmpty()) {
            alphabet + if (moduleType == ModuleType.MATHS) counting + shapes else emptyList()
        } else {
            (alphabet + if (moduleType == ModuleType.MATHS) counting + shapes else emptyList())
                .filter { it.id in itemIds }
        }
        if (pool.isEmpty()) return emptyList()

        // A single-lesson detail screen asks several *different* questions about
        // the same letter, so sampling is with replacement when the pool is
        // smaller than the requested question count.
        val chosen = if (pool.size >= count) {
            pool.shuffled(random).take(count)
        } else {
            (0 until count).map { pool[random.nextInt(pool.size)] }
        }

        return chosen.mapIndexedNotNull { index, item ->
            when (item) {
                is LetterItem -> letterQuestion(item, moduleType, random, index)
                is CountingItem -> countingQuestion(item, moduleType, random)
                is ShapeItem -> shapeQuestion(item, moduleType, random)
            }
        }
    }

    private fun letterQuestion(
        item: LetterItem,
        moduleType: ModuleType,
        random: Random,
        index: Int,
    ): QuizQuestion {
        // Alternate the two directions so a mixed quiz is not all one drill, but
        // always show the first question as "tap the letter" (the primary skill).
        val kind = if (index % 2 == 0) QuizKind.PICTURE_TO_LETTER else QuizKind.LETTER_TO_PICTURE
        return if (kind == QuizKind.PICTURE_TO_LETTER) {
            pictureToLetter(item, moduleType, random)
        } else {
            letterToPicture(item, moduleType, random)
        }
    }

    private fun pictureToLetter(item: LetterItem, moduleType: ModuleType, random: Random): QuizQuestion {
        val pool = lettersFor(moduleType).filter { it.letter != item.letter }
        val distractors = pickDistinct(pool, random, OPTION_COUNT - 1) { it.letter != item.letter }
        val options = (distractors + item).shuffled(random)
        return QuizQuestion(
            id = "q_pic2let_${item.id}",
            moduleType = moduleType,
            kind = QuizKind.PICTURE_TO_LETTER,
            promptVisual = item.visual,
            promptLabel = null,
            options = options.map { AnswerOption(it.id, it.letter, null) },
            correctIndex = options.indexOfFirst { it.id == item.id },
            speakText = "${item.exampleWord}",
            speakLocale = ttsLocale(moduleType),
            subjectItemId = item.id,
        )
    }

    private fun letterToPicture(item: LetterItem, moduleType: ModuleType, random: Random): QuizQuestion {
        // Reject same-emoji distractors so two visually identical buttons cannot
        // both look right, and reject the answer itself.
        val pool = lettersFor(moduleType).filter { it.id != item.id }
        val distractors = pickDistinct(pool, random, OPTION_COUNT - 1) {
            it.letter != item.letter && it.visual != item.visual
        }
        val options = (distractors + item).shuffled(random)
        // Options show the native word + picture, never the latin transliteration:
        // a Bangla/Arabic child cannot read "kho"/"gh", and showing latin next to
        // the emoji is exactly the "image not matched" parents reported.
        // Speech reuses the exact lesson phrasing ([lessonSpeechText]) so every
        // letter has a single bundled narration clip: the prompt already displays
        // the letter, so speaking it reveals nothing extra.
        val spoken = lessonSpeechText(moduleType, item.letter, item.secondary, item.exampleWord)
        return QuizQuestion(
            id = "q_let2pic_${item.id}",
            moduleType = moduleType,
            kind = QuizKind.LETTER_TO_PICTURE,
            promptVisual = "",
            promptLabel = item.letter,
            options = options.map { AnswerOption(it.id, it.exampleWord, it.visual) },
            correctIndex = options.indexOfFirst { it.id == item.id },
            speakText = spoken,
            speakLocale = ttsLocale(moduleType),
            subjectItemId = item.id,
        )
    }

    private fun countingQuestion(
        item: CountingItem,
        moduleType: ModuleType,
        random: Random,
    ): QuizQuestion {
        val isArithmetic = random.nextInt(100) < ARITHMETIC_CHANCE_PERCENT
        return if (isArithmetic) arithmeticQuestion(item, moduleType, random)
        else plainCountingQuestion(item, moduleType, random)
    }

    private fun plainCountingQuestion(
        item: CountingItem,
        moduleType: ModuleType,
        random: Random,
    ): QuizQuestion {
        val options = numberOptions(item.number, random)
        return QuizQuestion(
            id = "q_count_${item.id}",
            moduleType = moduleType,
            kind = QuizKind.COUNT_OBJECTS,
            promptVisual = item.repeatedVisual,
            promptLabel = null,
            options = options.map { AnswerOption("n$it", it.toString(), null) },
            correctIndex = options.indexOf(item.number),
            speakText = "How many?",
            speakLocale = ttsLocale(moduleType),
            subjectItemId = item.id,
        )
    }

    /**
     * Single-digit addition / subtraction.
     *
     * Subtraction is clamped so the answer is never negative - a negative answer
     * is meaningless to this age group and would need explaining.
     */
    private fun arithmeticQuestion(
        item: CountingItem,
        moduleType: ModuleType,
        random: Random,
    ): QuizQuestion {
        val a = (1..MAX_ARITHMETIC).random(random)
        val isAddition = random.nextBoolean()
        val b = (1..MAX_ARITHMETIC).random(random)
        val answer = if (isAddition) a + b else a - b

        return if (answer < 0) {
            // Roll again rather than showing a negative number.
            plainCountingQuestion(item, moduleType, random)
        } else {
            val options = numberOptions(answer, random)
            QuizQuestion(
                id = "q_math_${item.id}_${if (isAddition) "add" else "sub"}",
                moduleType = moduleType,
                kind = if (isAddition) QuizKind.ADDITION else QuizKind.SUBTRACTION,
                promptVisual = "",
                promptLabel = if (isAddition) "$a + $b" else "$a − $b",
                options = options.map { AnswerOption("n$it", it.toString(), null) },
                correctIndex = options.indexOf(answer),
                speakText = if (isAddition) "$a plus $b" else "$a minus $b",
                speakLocale = ttsLocale(moduleType),
                subjectItemId = item.id,
            )
        }
    }

    private fun shapeQuestion(
        item: ShapeItem,
        moduleType: ModuleType,
        random: Random,
    ): QuizQuestion {
        val pool = shapes.filter { it.id != item.id }
        val distractors = pickDistinct(pool, random, OPTION_COUNT - 1) { it.visual != item.visual }
        val options = (distractors + item).shuffled(random)
        return QuizQuestion(
            id = "q_shape_${item.id}",
            moduleType = moduleType,
            kind = QuizKind.SHAPE_COLOUR,
            promptVisual = item.visual,
            promptLabel = null,
            options = options.map { AnswerOption(it.id, it.shapeName, null) },
            correctIndex = options.indexOfFirst { it.id == item.id },
            speakText = item.shapeName,
            speakLocale = ttsLocale(moduleType),
            subjectItemId = item.id,
        )
    }

    /**
     * Picks `n` candidates that pass [isValid].
     *
     * If fewer than `n` candidates qualify, it returns *all* of them rather than
     * falling back to the unfiltered pool. Widening the search would risk handing
     * back the very item the caller is using as the answer, and the caller appends
     * the answer separately - so a short list just means a question with fewer
     * options, which is always better than a question with no correct answer.
     */
    private fun <T> pickDistinct(
        pool: List<T>,
        random: Random,
        n: Int,
        isValid: (T) -> Boolean,
    ): List<T> {
        val valid = pool.filter(isValid)
        return valid.shuffled(random).take(n)
    }

    /**
     * [correct] plus nearby distractors, shuffled.
     *
     * The answer is appended *after* the distractors are chosen and truncated, then
     * the whole list is shuffled. Shuffling first and truncating second - the
     * obvious-looking implementation - can drop the correct value entirely and
     * produce a question with no right answer, which for this audience is the worst
     * possible bug and the hardest to spot in review.
     *
     * Neighbours are used as distractors so the options are plausible; anything
     * outside the taught 0-20 range is dropped, and a short list is padded by
     * widening the search only among in-range values.
     */
    private fun numberOptions(correct: Int, random: Random): List<Int> {
        val distractors = buildList {
            addAll(listOf(correct + 1, correct - 1, correct + 2, correct - 2, correct + 3))
            // Widen if the neighbours fell out of range, e.g. near 0 or 20.
            addAll(listOf(correct + 4, correct - 4, correct + 5, correct - 5, correct + 6, correct - 6))
        }
            .filter { it in MIN_NUMBER..MAX_NUMBER && it != correct }
            .distinct()
            .shuffled(random)
            .take(OPTION_COUNT - 1)

        return (distractors + correct).shuffled(random)
    }

    companion object {
        const val OPTION_COUNT = 4
        const val MIN_NUMBER = 0
        const val MAX_NUMBER = 20
        const val MAX_ARITHMETIC = 5

        /** Chance a counting lesson becomes an arithmetic question. */
        const val ARITHMETIC_CHANCE_PERCENT = 40

        /**
         * BCP-47 tags for the offline text-to-speech engine.
         *
         * `bn-BD` / `ar-SA` are regional pins: a generic `bn` may resolve to a
         * Bengali-India voice with different pronunciation conventions.
         */
        fun ttsLocale(moduleType: ModuleType): String = when (moduleType) {
            ModuleType.ENGLISH -> "en-US"
            ModuleType.BANGLA -> "bn-BD"
            ModuleType.ARABIC -> "ar-SA"
            ModuleType.MATHS -> "en-US"
        }
    }
}
