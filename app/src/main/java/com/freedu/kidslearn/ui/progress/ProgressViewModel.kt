package com.freedu.kidslearn.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The progress dashboard.
 *
 * Deliberately read-only: a child can see what they have achieved but cannot change
 * it. The only actions that alter progress live behind the parent zone's maths
 * gate, which is the single place where "is this person a parent?" is verified.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    statsRepository: StatsRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardSnapshot> = statsRepository.observeDashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            // A non-empty placeholder so the first frame already has four bars in
            // the right places instead of flashing an empty screen.
            initialValue = DashboardSnapshot(
                modules = com.freedu.kidslearn.domain.model.ModuleType.entries.map { module ->
                    com.freedu.kidslearn.domain.model.ModuleProgress(
                        moduleType = module,
                        totalItems = 0,
                        completedItems = 0,
                        totalStars = 0,
                        maxStars = 0,
                    )
                },
            ),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
