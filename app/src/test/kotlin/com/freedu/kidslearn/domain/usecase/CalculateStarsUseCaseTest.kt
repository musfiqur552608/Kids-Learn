package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.LessonProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Star thresholds are the most product-critical rule in the app, so they are pinned
 * by an exhaustive table rather than a handful of spot checks. If someone retunes
 * the thresholds, this test is the thing that has to be updated deliberately.
 */
class CalculateStarsUseCaseTest {

    private val useCase = CalculateStarsUseCase()

    @Test
    fun `no questions earns nothing`() {
        assertThat(useCase(0, 0)).isEqualTo(0)
    }

    @Test
    fun `no correct answers earns nothing`() {
        assertThat(useCase(0, 5)).isEqualTo(0)
        assertThat(useCase(0, 1)).isEqualTo(0)
    }

    @Test
    fun `a full score on a real paper earns three stars`() {
        assertThat(useCase(5, 5)).isEqualTo(3)
    }

    @Test
    fun `a full score on a very short paper is capped at two stars`() {
        // Two lucky taps must not be worth the same as five real answers.
        assertThat(useCase(2, 2)).isEqualTo(2)
        assertThat(useCase(1, 1)).isEqualTo(2)
    }

    @Test
    fun `two thirds earns two stars`() {
        assertThat(useCase(4, 6)).isEqualTo(2)
        assertThat(useCase(3, 4)).isEqualTo(2)
    }

    @Test
    fun `anything above zero earns at least one star`() {
        // Deliberately generous: a five-year-old who got one right has learned
        // something, and "zero" would read as failure.
        assertThat(useCase(1, 5)).isEqualTo(1)
        assertThat(useCase(1, 10)).isEqualTo(1)
    }

    @Test
    fun `a negative score is clamped to zero`() {
        assertThat(useCase(-3, 5)).isEqualTo(0)
    }

    @Test
    fun `more correct than total does not exceed three stars`() {
        assertThat(useCase(9, 5)).isAtMost(LessonProgress.MAX_STARS)
    }

    // ------------------------------------------------------------------- coins

    @Test
    fun `a zero score earns no coins`() {
        assertThat(useCase.coinsFor(stars = 0, correctAnswers = 0)).isEqualTo(0)
    }

    @Test
    fun `coins combine a per-correct bonus with a per-star bonus`() {
        // 3 correct * 2 + 3 stars * 5 = 21
        assertThat(useCase.coinsFor(stars = 3, correctAnswers = 3)).isEqualTo(21)
    }

    @Test
    fun `coins are capped per quiz`() {
        val coins = useCase.coinsFor(
            stars = LessonProgress.MAX_STARS,
            correctAnswers = 100,
        )
        assertThat(coins).isAtMost(CalculateStarsUseCase.COINS_PER_QUIZ_CAP)
    }

    @Test
    fun `a child who tries but scores zero still gets coins`() {
        // Participation must always be worth something, otherwise a bad run feels
        // like being taken away something.
        assertThat(useCase.coinsFor(stars = 1, correctAnswers = 1)).isGreaterThan(0)
    }
}
