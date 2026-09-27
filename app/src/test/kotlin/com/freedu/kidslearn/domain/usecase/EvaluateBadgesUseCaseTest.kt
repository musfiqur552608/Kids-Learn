package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UserStats
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EvaluateBadgesUseCaseTest {

    private val useCase = EvaluateBadgesUseCase()

    private fun module(type: ModuleType, stars: Int) = ModuleProgress(
        moduleType = type,
        totalItems = 26,
        completedItems = 1,
        totalStars = stars,
        maxStars = 78,
    )

    @Test
    fun `nothing is earned from an empty profile`() {
        val earned = useCase(UserStats.EMPTY, emptyList(), emptySet())

        assertThat(earned).isEmpty()
    }

    @Test
    fun `the first completed lesson unlocks FIRST_STEP`() {
        val stats = UserStats(lessonsCompleted = 1)

        val earned = useCase(stats, emptyList(), emptySet())

        assertThat(earned).contains(BadgeKey.FIRST_STEP)
    }

    @Test
    fun `three stars anywhere unlocks FIRST_PERFECT`() {
        val stats = UserStats(totalStars = 3, lessonsCompleted = 3)

        val earned = useCase(stats, emptyList(), emptySet())

        assertThat(earned).contains(BadgeKey.FIRST_PERFECT)
    }

    @Test
    fun `a module badge needs its own module's stars`() {
        val earned = useCase(
            stats = UserStats(totalStars = 30),
            moduleProgress = listOf(module(ModuleType.ENGLISH, 10), module(ModuleType.MATHS, 9)),
            alreadyUnlocked = emptySet(),
        )

        assertThat(earned).contains(BadgeKey.ENGLISH_STAR)
        assertThat(earned).doesNotContain(BadgeKey.BANGLA_STAR)
        assertThat(earned).doesNotContain(BadgeKey.MATHS_STAR)
    }

    @Test
    fun `each module badge is independent`() {
        val modules = ModuleType.entries.map { module(it, 10) }

        val earned = useCase(UserStats(totalStars = 40), moduleProgress = modules, alreadyUnlocked = emptySet())

        assertThat(earned).containsAtLeast(
            BadgeKey.ENGLISH_STAR,
            BadgeKey.BANGLA_STAR,
            BadgeKey.ARABIC_STAR,
            BadgeKey.MATHS_STAR,
        )
    }

    @Test
    fun `an already unlocked badge is never returned again`() {
        val stats = UserStats(totalStars = 30, lessonsCompleted = 1, longestStreak = 7)

        val earned = useCase(stats, emptyList(), BadgeKey.entries.toSet())

        assertThat(earned).isEmpty()
    }

    @Test
    fun `streak badges use the longest streak not the current one`() {
        // A child who once hit 7 days should keep that badge even if the current
        // streak has since broken.
        val stats = UserStats(currentStreak = 0, longestStreak = 7)

        val earned = useCase(stats, emptyList(), emptySet())

        assertThat(earned).contains(BadgeKey.STREAK_7)
        assertThat(earned).contains(BadgeKey.STREAK_3)
    }

    @Test
    fun `collector badges fire at their exact thresholds`() {
        assertThat(useCase(UserStats(totalStars = 9), emptyList(), emptySet()))
            .doesNotContain(BadgeKey.COLLECTOR_10)
        assertThat(useCase(UserStats(totalStars = 10), emptyList(), emptySet()))
            .contains(BadgeKey.COLLECTOR_10)
        assertThat(useCase(UserStats(totalStars = 24), emptyList(), emptySet()))
            .doesNotContain(BadgeKey.COLLECTOR_25)
        assertThat(useCase(UserStats(totalStars = 25), emptyList(), emptySet()))
            .contains(BadgeKey.COLLECTOR_25)
    }

    @Test
    fun `the game champion badge needs five games played`() {
        assertThat(useCase(UserStats(gamesPlayed = 4), emptyList(), emptySet()))
            .doesNotContain(BadgeKey.GAME_CHAMPION)
        assertThat(useCase(UserStats(gamesPlayed = 5), emptyList(), emptySet()))
            .contains(BadgeKey.GAME_CHAMPION)
    }

    @Test
    fun `results are returned in a stable order`() {
        val stats = UserStats(totalStars = 40, lessonsCompleted = 5, longestStreak = 7, gamesPlayed = 5)

        val first = useCase(stats, emptyList(), emptySet())
        val second = useCase(stats, emptyList(), emptySet())

        assertThat(first).isEqualTo(second)
    }
}
