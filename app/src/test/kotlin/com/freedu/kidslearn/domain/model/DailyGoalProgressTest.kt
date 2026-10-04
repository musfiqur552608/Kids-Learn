package com.freedu.kidslearn.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins the one-number goal semantics: met at exactly the target, remaining
 * never negative (the banner divides by the goal, so overshoot must be safe).
 */
class DailyGoalProgressTest {

    @Test
    fun `met exactly at the target`() {
        assertThat(DailyGoalProgress(goal = 3, doneToday = 2).met).isFalse()
        assertThat(DailyGoalProgress(goal = 3, doneToday = 3).met).isTrue()
        assertThat(DailyGoalProgress(goal = 3, doneToday = 9).met).isTrue()
    }

    @Test
    fun `remaining floors at zero`() {
        assertThat(DailyGoalProgress(goal = 3, doneToday = 1).remaining).isEqualTo(2)
        assertThat(DailyGoalProgress(goal = 3, doneToday = 3).remaining).isEqualTo(0)
        assertThat(DailyGoalProgress(goal = 3, doneToday = 9).remaining).isEqualTo(0)
    }
}
