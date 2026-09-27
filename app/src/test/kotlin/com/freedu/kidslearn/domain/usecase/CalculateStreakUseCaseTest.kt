package com.freedu.kidslearn.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Streak arithmetic, tested exhaustively.
 *
 * The two properties that matter are: a streak survives a couple of missed days
 * (see [CalculateStreakUseCase.GENTLE_GRACE_DAYS]) and re-opening the app on the
 * same day never inflates it. The second is what makes it safe to call
 * `touchActivity` from both `Application.onCreate` and Home resume.
 */
class CalculateStreakUseCaseTest {

    private val useCase = CalculateStreakUseCase()

    @Test
    fun `first ever session starts a streak of one`() {
        val result = useCase(previousStreak = 0, longestStreak = 0, lastActiveEpochDay = null, todayEpochDay = 100)

        assertThat(result.currentStreak).isEqualTo(1)
        assertThat(result.longestStreak).isEqualTo(1)
    }

    @Test
    fun `consecutive days extend the streak`() {
        val result = useCase(previousStreak = 3, longestStreak = 3, lastActiveEpochDay = 100, todayEpochDay = 101)

        assertThat(result.currentStreak).isEqualTo(4)
        assertThat(result.longestStreak).isEqualTo(4)
    }

    @Test
    fun `opening twice on the same day does not double count`() {
        val result = useCase(previousStreak = 4, longestStreak = 7, lastActiveEpochDay = 100, todayEpochDay = 100)

        assertThat(result.currentStreak).isEqualTo(4)
        assertThat(result.longestStreak).isEqualTo(7)
    }

    @Test
    fun `a gap inside the grace period keeps the streak`() {
        // Two days missed is inside the grace window.
        val result = useCase(previousStreak = 5, longestStreak = 5, lastActiveEpochDay = 100, todayEpochDay = 103)

        assertThat(result.currentStreak).isEqualTo(6)
    }

    @Test
    fun `a gap beyond the grace period resets to one`() {
        val gap = CalculateStreakUseCase.GENTLE_GRACE_DAYS + 2
        val result = useCase(previousStreak = 5, longestStreak = 9, lastActiveEpochDay = 100, todayEpochDay = 100 + gap)

        assertThat(result.currentStreak).isEqualTo(1)
        // The personal best survives a reset, otherwise the record is lost forever.
        assertThat(result.longestStreak).isEqualTo(9)
    }

    @Test
    fun `a clock moving backwards resets rather than going negative`() {
        val result = useCase(previousStreak = 6, longestStreak = 6, lastActiveEpochDay = 200, todayEpochDay = 100)

        assertThat(result.currentStreak).isEqualTo(1)
        assertThat(result.currentStreak).isAtLeast(0)
    }

    @Test
    fun `longest streak is never reduced by a current streak that shrank`() {
        val result = useCase(previousStreak = 1, longestStreak = 12, lastActiveEpochDay = 100, todayEpochDay = 101)

        assertThat(result.longestStreak).isEqualTo(12)
    }

    @Test
    fun `a very long streak keeps counting`() {
        val result = useCase(previousStreak = 99, longestStreak = 99, lastActiveEpochDay = 0, todayEpochDay = 1)

        assertThat(result.currentStreak).isEqualTo(100)
    }
}
