package com.freedu.kidslearn.domain.usecase

import javax.inject.Inject

/**
 * Daily-streak arithmetic, extracted from persistence so it can be tested
 * exhaustively without a database.
 *
 * ## Semantics chosen for young children
 * * Opening the app on consecutive calendar days extends the streak.
 * * Missing a day resets the streak to 1 (today still counts as a day played).
 * * [GENTLE_GRACE_DAYS]-day grace: a streak survives a couple of missed days.
 *   Without this, a five-year-old who misses two afternoons in a row sees their
 *   streak collapse to 1, which reads as punishment. The requirement of "positive
 *   only" feedback pushes us to be forgiving here.
 * * The streak never exceeds [longestStreak] and both are stored so the dashboard
 *   can show a personal best without a full history scan.
 */
class CalculateStreakUseCase @Inject constructor() {

    data class Result(
        val currentStreak: Int,
        val longestStreak: Int,
    )

    operator fun invoke(
        previousStreak: Int,
        longestStreak: Int,
        lastActiveEpochDay: Long?,
        todayEpochDay: Long,
    ): Result {
        // First ever session.
        if (lastActiveEpochDay == null) {
            return Result(currentStreak = 1, longestStreak = maxOf(1, longestStreak))
        }

        val daysSinceLastActive = todayEpochDay - lastActiveEpochDay

        return when {
            // Already counted today: opening the app again must not inflate the
            // streak. This is what makes `touchActivity` safe to call repeatedly.
            daysSinceLastActive == 0L -> Result(previousStreak, maxOf(previousStreak, longestStreak))

            daysSinceLastActive < 0L ->
                // Clock moved backwards (timezone change, NTP correction). Treat
                // the streak as broken rather than decrementing into nonsense.
                Result(currentStreak = 1, longestStreak = maxOf(1, longestStreak))

            daysSinceLastActive <= GENTLE_GRACE_DAYS + 1L -> {
                val extended = previousStreak.coerceAtLeast(0) + 1
                Result(currentStreak = extended, longestStreak = maxOf(extended, longestStreak))
            }

            else -> Result(currentStreak = 1, longestStreak = maxOf(1, longestStreak))
        }
    }

    companion object {
        /** Missed days tolerated before the streak resets. */
        const val GENTLE_GRACE_DAYS = 2L
    }
}
