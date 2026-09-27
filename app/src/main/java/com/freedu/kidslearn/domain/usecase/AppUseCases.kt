package com.freedu.kidslearn.domain.usecase

import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UserStats
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Records "the child opened the app today" and updates the daily streak.
 *
 * Invoked from `KidsLearnApplication.onCreate` rather than from a Composable: the
 * streak must be correct even for a cold start that restores straight into a
 * lesson, which a Home-screen-only hook would miss.
 */
class TouchActivityUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): UserStats =
        statsRepository.touchActivity(today)
}

/**
 * Wipes every trace of progress.
 *
 * Deliberately takes no confirmation flag: the parent-zone maths gate *is* the
 * confirmation, and a second "are you sure?" dialog in a parent-facing screen is
 * just friction.
 */
class ResetProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val statsRepository: StatsRepository,
) {
    suspend operator fun invoke() {
        progressRepository.clearAll()
        statsRepository.resetAll()
    }
}

/**
 * Remembers the module a child last opened so Home can offer "continue where you
 * left off".
 */
class RememberLastModuleUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(module: ModuleType) = settingsRepository.setLastModule(module)
}
