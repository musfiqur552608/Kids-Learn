package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.LessonProgress
import javax.inject.Inject

/**
 * Converts a raw score into the 1-3 stars a child sees.
 *
 * ## Why this is a use case and not a private function
 * Star thresholds are the single most business-critical rule in the app: parents
 * judge the product by them and they appear on the dashboard, in Room, and in the
 * celebration animation. Isolating them in pure Kotlin means the whole policy is
 * unit-testable on the JVM with no Android, no Room and no coroutines.
 *
 * ## Why the thresholds are generous
 * The target audience is 3-8 year olds who are still developing fine motor
 * control. A 50% pass mark feels like failure to a five-year-old, so partial
 * credit starts after a *single* correct answer. [MIN_QUESTIONS_FOR_STARS] stops
 * a two-question quiz from handing out three stars for two lucky guesses.
 */
class CalculateStarsUseCase @Inject constructor() {

    operator fun invoke(correctAnswers: Int, totalQuestions: Int): Int {
        if (totalQuestions <= 0 || correctAnswers <= 0) return 0
        if (correctAnswers >= totalQuestions && totalQuestions >= MIN_QUESTIONS_FOR_STARS) {
            return LessonProgress.MAX_STARS
        }
        val ratio = correctAnswers.toFloat() / totalQuestions
        return when {
            ratio >= TWO_STAR_THRESHOLD -> 2
            else -> 1
        }
    }

    /**
     * Coins awarded for a finished quiz: a small amount for participation so a
     * child is never left with zero, plus a bonus per star for mastery.
     */
    fun coinsFor(stars: Int, correctAnswers: Int): Int {
        if (stars <= 0 && correctAnswers <= 0) return 0
        val participation = correctAnswers * COINS_PER_CORRECT
        val mastery = stars * COINS_PER_STAR
        return (participation + mastery).coerceAtMost(COINS_PER_QUIZ_CAP)
    }

    companion object {
        /** Below this many questions a perfect score is treated as "not enough". */
        const val MIN_QUESTIONS_FOR_STARS = 3

        /** 2/3 correct earns 2 stars. */
        const val TWO_STAR_THRESHOLD = 0.66f

        const val COINS_PER_CORRECT = 2
        const val COINS_PER_STAR = 5
        const val COINS_PER_QUIZ_CAP = 40
    }
}
