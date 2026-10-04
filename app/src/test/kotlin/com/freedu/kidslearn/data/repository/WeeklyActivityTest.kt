package com.freedu.kidslearn.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Guards the parent-zone weekly chart bucketing.
 *
 * The chart must always show exactly seven days ending today - even with no
 * history - or its shape shifts under the parent. Out-of-window and null
 * dates are ignored rather than clamped, so one old row cannot inflate a bar.
 */
class WeeklyActivityTest {

    private val today = LocalDate.of(2026, 10, 4) // a Sunday

    @Test
    fun `empty history still yields seven dated days ending today`() {
        val weekly = bucketWeekly(emptyList(), emptyList(), today)

        assertThat(weekly.days).hasSize(7)
        assertThat(weekly.days.first().date).isEqualTo(today.minusDays(6))
        assertThat(weekly.days.last().date).isEqualTo(today)
        assertThat(weekly.totalLessons).isEqualTo(0)
        assertThat(weekly.totalGames).isEqualTo(0)
    }

    @Test
    fun `lessons and games land on their own days`() {
        val weekly = bucketWeekly(
            lessonDates = listOf(today, today, today.minusDays(2), null),
            gameDates = listOf(today.minusDays(2), today.minusDays(6)),
            today = today,
        )

        assertThat(weekly.days.last()).isEqualTo(
            com.freedu.kidslearn.domain.model.DayActivity(today, lessons = 2, games = 0),
        )
        assertThat(weekly.days[4]).isEqualTo(
            com.freedu.kidslearn.domain.model.DayActivity(today.minusDays(2), lessons = 1, games = 1),
        )
        assertThat(weekly.days.first().games).isEqualTo(1)
        assertThat(weekly.totalLessons).isEqualTo(3)
        assertThat(weekly.totalGames).isEqualTo(2)
    }

    @Test
    fun `dates outside the window are ignored`() {
        val weekly = bucketWeekly(
            lessonDates = listOf(today.minusDays(7), today.minusDays(30), today.plusDays(1)),
            gameDates = listOf(today.minusDays(8)),
            today = today,
        )

        assertThat(weekly.totalLessons).isEqualTo(0)
        assertThat(weekly.totalGames).isEqualTo(0)
    }
}
