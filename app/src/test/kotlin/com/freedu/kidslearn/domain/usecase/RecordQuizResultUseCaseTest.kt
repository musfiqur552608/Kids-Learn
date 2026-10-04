package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizKind
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.testing.FakeProgressRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * End-to-end test of the scoring transaction, with the repositories faked.
 *
 * The point is to verify the *ordering* as well as the arithmetic: badges must be
 * evaluated after the stars and coins have been written, or a child would have to
 * play a second lesson to collect the badge the first one earned.
 */
class RecordQuizResultUseCaseTest {

    private val repository = FakeProgressRepository()
    private val useCase = RecordQuizResultUseCase(
        progressRepository = repository,
        statsRepository = repository,
        calculateStars = CalculateStarsUseCase(),
        evaluateBadges = EvaluateBadgesUseCase(),
    )

    private val today: LocalDate = LocalDate.of(2026, 9, 27)

    @Test
    fun `a perfect quiz records every covered lesson`() = runTest {
        val result = useCase(ModuleType.ENGLISH, listOf("ENGLISH_A", "ENGLISH_B"), 3, 3, today)

        assertThat(result.starsEarned).isEqualTo(3)
        assertThat(result.correctAnswers).isEqualTo(3)
        assertThat(repository.currentLesson(ModuleType.ENGLISH, "ENGLISH_A")?.starsEarned).isEqualTo(3)
        assertThat(repository.currentLesson(ModuleType.ENGLISH, "ENGLISH_B")?.starsEarned).isEqualTo(3)
    }

    @Test
    fun `stars and coins reach the lifetime totals`() = runTest {
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A", "ENGLISH_B"), 3, 3, today)

        val stats = repository.currentStats()
        // 3 stars per lesson * 2 lessons.
        assertThat(stats.totalStars).isEqualTo(6)
        assertThat(stats.totalCoins).isGreaterThan(0)
        assertThat(stats.lessonsCompleted).isEqualTo(2)
    }

    @Test
    fun `a partial score still records progress`() = runTest {
        val result = useCase(ModuleType.MATHS, listOf("MATHS_COUNT_01"), 1, 3, today)

        assertThat(result.starsEarned).isEqualTo(1)
        assertThat(repository.currentLesson(ModuleType.MATHS, "MATHS_COUNT_01")?.isCompleted).isTrue()
    }

    @Test
    fun `a reattempt never lowers an earlier star count`() = runTest {
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 3, 3, today)
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 0, 3, today)

        assertThat(repository.currentLesson(ModuleType.ENGLISH, "ENGLISH_A")?.starsEarned).isEqualTo(3)
    }

    @Test
    fun `lessons completed counts distinct lessons not attempts`() = runTest {
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 3, 3, today)
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 3, 3, today)
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 3, 3, today)

        assertThat(repository.currentStats().lessonsCompleted).isEqualTo(1)
    }

    @Test
    fun `the first lesson unlocks its badge immediately`() = runTest {
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 1, 1, today)

        // FIRST_STEP and FIRST_PERFECT both require at most one lesson with stars.
        assertThat(repository.currentBadges()).containsAtLeast(
            com.freedu.kidslearn.domain.model.BadgeKey.FIRST_STEP,
            com.freedu.kidslearn.domain.model.BadgeKey.FIRST_PERFECT,
        )
    }

    @Test
    fun `a badge is unlocked only once however many times it is earned`() = runTest {
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 3, 3, today)
        val afterFirst = repository.currentBadges().size
        useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 3, 3, today)

        assertThat(repository.currentBadges().size).isEqualTo(afterFirst)
    }

    @Test
    fun `an empty item list records scores but writes no lesson rows`() = runTest {
        useCase(ModuleType.MATHS, itemIds = emptyList(), correctAnswers = 4, totalQuestions = 5, today = today)

        assertThat(repository.currentStats().lessonsCompleted).isEqualTo(0)
    }

    @Test
    fun `a scoreless quiz marks nothing learned`() = runTest {
        val result = useCase(ModuleType.ENGLISH, listOf("ENGLISH_A"), 0, 3, today)

        assertThat(result.starsEarned).isEqualTo(0)
        assertThat(repository.currentLesson(ModuleType.ENGLISH, "ENGLISH_A")?.isCompleted).isFalse()
        assertThat(repository.currentStats().lessonsCompleted).isEqualTo(0)
        assertThat(repository.currentBadges()).isEmpty()
    }
}
