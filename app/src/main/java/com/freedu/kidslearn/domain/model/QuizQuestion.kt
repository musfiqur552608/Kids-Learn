package com.freedu.kidslearn.domain.model

/**
 * The different question shapes the app can ask.
 *
 * Keeping this in the domain (rather than letting each screen invent its own
 * question type) is what allows a single reusable [QuizViewModel] to drive the
 * English, Bangla, Arabic and maths quizzes.
 */
enum class QuizKind {
    /** Shows a picture, child taps the letter that names it. */
    PICTURE_TO_LETTER,

    /** Shows a letter, child taps the picture it names. */
    LETTER_TO_PICTURE,

    /** Shows N objects, child taps the number. */
    COUNT_OBJECTS,

    /** Shows a single-digit sum, child taps the answer. */
    ADDITION,

    /** Shows a single-digit difference, child taps the answer. */
    SUBTRACTION,

    /** Shows a shape and/or colour, child taps the name. */
    SHAPE_COLOUR,
}

/**
 * One selectable answer.
 *
 * @param label the text to render, e.g. `"A"` or `"4"`
 * @param visual optional emoji shown instead of / above the label
 */
data class AnswerOption(
    val id: String,
    val label: String,
    val visual: String? = null,
)

/**
 * A single quiz question.
 *
 * @param speakText what the pronunciation engine should say. For a letter quiz
 *   this is just the letter plus the example word, e.g. `"A, A for Apple"`.
 * @param speakLocale BCP-47 tag used for offline text-to-speech, e.g. `"bn-BD"`.
 */
data class QuizQuestion(
    val id: String,
    val moduleType: ModuleType,
    val kind: QuizKind,
    /** Big visual shown as the prompt (emoji, or repeated emoji for counting). */
    val promptVisual: String,
    /** Optional textual prompt such as `"3 + 2"`. */
    val promptLabel: String? = null,
    val options: List<AnswerOption>,
    val correctIndex: Int,
    val speakText: String,
    val speakLocale: String,
    /**
     * The lesson this question teaches, for progress recording.
     *
     * A letter question's subject is its letter's item id; a counting question's
     * is its [CountingItem] id (arithmetic inherits its spawning item's id); a
     * shape question's is its [ShapeItem] id. The quiz reports exactly the
     * subjects answered correctly, which is what makes "X of 26 learned" move
     * after a mixed quiz - and what stops a failed quiz from marking anything.
     */
    val subjectItemId: String,
) {
    init {
        require(options.size >= 2) { "A quiz question needs at least 2 options" }
        require(correctIndex in options.indices) {
            "correctIndex=$correctIndex is out of bounds for ${options.size} options"
        }
    }

    val correctOption: AnswerOption get() = options[correctIndex]
}

/** How a child answered one question, used to drive gentle feedback. */
data class AnswerOutcome(
    val questionId: String,
    val selectedIndex: Int?,
    val isCorrect: Boolean,
    /** Number of wrong attempts before the correct one (0 = first try). */
    val mistakes: Int = 0,
)
