package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.QuizResult
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * The transactional heart of the app: turns a finished quiz into persisted
 * progress, coins, stats and badges.
 *
 * ## Why one use case instead of the ViewModel writing directly
 * A single screen action fans out to four tables plus two badge checks. Doing
 * that inside a ViewModel would make the rules untestable (they would need a
 * ViewModel, a coroutine scope and a database) and would spread the business rules
 * across the UI layer. Here the whole transaction is one pure-ish function of its
 * inputs that a unit test can drive with fake repositories.
 *
 * ## Ordering
 * 1. write lesson rows, 2. add stars/coins, 3. recompute badges.
 * Badges read `UserStats` and the per-module roll-up, so they must run last or
 * they would evaluate against pre-update numbers.
 */
class RecordQuizResultUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val statsRepository: StatsRepository,
    private val calculateStars: CalculateStarsUseCase,
    private val evaluateBadges: EvaluateBadgesUseCase,
) {

    suspend operator fun invoke(
        moduleType: com.freedu.kidslearn.domain.model.ModuleType,
        itemIds: List<String>,
        correctAnswers: Int,
        totalQuestions: Int,
        today: LocalDate,
    ): QuizResult {
        val stars = calculateStars(correctAnswers, totalQuestions)
        val coins = calculateStars.coinsFor(stars, correctAnswers)

        itemIds.forEach { itemId ->
            progressRepository.recordLesson(
                moduleType = moduleType,
                itemId = itemId,
                starsEarned = stars,
                today = today,
            )
        }

        statsRepository.addStars(stars * itemIds.size.coerceAtLeast(1))
        statsRepository.addCoins(coins)

        val newlyUnlocked = evaluateAndUnlock(today)
        val isNewPersonalBest = stars > 0 && newlyUnlocked.isNotEmpty()

        return QuizResult(
            moduleType = moduleType,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            starsEarned = stars,
            coinsEarned = coins,
            itemIds = itemIds,
            isNewPersonalBest = isNewPersonalBest,
        )
    }

    /** Shared with [RecordGameResultUseCase]; both are the only badge writers. */
    private suspend fun evaluateAndUnlock(today: LocalDate): List<com.freedu.kidslearn.domain.model.BadgeKey> {
        val stats = statsRepository.observeStats().first()
        val modules: List<ModuleProgress> = statsRepository.observeModuleProgress().first()
        val unlocked = statsRepository.observeBadges().first().map { it.badgeKey }.toSet()
        val earned = evaluateBadges(stats, modules, unlocked)
        return if (earned.isEmpty()) emptyList() else statsRepository.unlockBadges(earned, today)
    }
}
