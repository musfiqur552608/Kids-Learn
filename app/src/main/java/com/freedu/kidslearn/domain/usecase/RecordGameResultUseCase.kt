package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.GameResult
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Persists a finished mini-game: score history, stars, coins, badges.
 *
 * Mirrors [RecordQuizResultUseCase] so both entry points into the gamification
 * system behave identically - a parent should not be able to tell from the
 * dashboard which screen earned the stars.
 */
class RecordGameResultUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val statsRepository: StatsRepository,
    private val calculateStars: CalculateStarsUseCase,
    private val evaluateBadges: EvaluateBadgesUseCase,
) {

    suspend operator fun invoke(
        gameType: GameType,
        score: Int,
        totalRounds: Int,
        correctAnswers: Int,
        today: LocalDate,
    ): GameResult {
        val stars = calculateStars(correctAnswers, totalRounds)
        val coins = calculateStars.coinsFor(stars, correctAnswers)

        // The personal-best check has to happen *before* the row is written, and
        // `recordGame` reads the previous best itself, so the two agree.
        val previousBest = progressRepository.observeBestScore(gameType).first()
        val isNewBest = score > previousBest

        progressRepository.recordGame(
            GameScore(
                gameType = gameType,
                score = score,
                starsEarned = stars,
                playedAt = today,
            ),
        )
        statsRepository.noteGamePlayed()
        statsRepository.addStars(stars)
        statsRepository.addCoins(coins)

        val stats = statsRepository.observeStats().first()
        val modules = statsRepository.observeModuleProgress().first()
        val unlocked = statsRepository.observeBadges().first().map { it.badgeKey }.toSet()
        val earned = evaluateBadges(stats, modules, unlocked)
        if (earned.isNotEmpty()) statsRepository.unlockBadges(earned, today)

        return GameResult(
            gameType = gameType,
            score = score,
            totalRounds = totalRounds,
            correctAnswers = correctAnswers,
            starsEarned = stars,
            coinsEarned = coins,
            isNewPersonalBest = isNewBest,
        )
    }
}
