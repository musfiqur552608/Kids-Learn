package com.freedu.kidslearn.ui.parentzone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.model.UiLanguage
import com.freedu.kidslearn.domain.model.WeeklyActivity
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.domain.repository.StatsRepository
import com.freedu.kidslearn.domain.usecase.GenerateParentGateUseCase
import com.freedu.kidslearn.domain.usecase.ResetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the parent is in the gate flow. */
sealed interface ParentGateState {
    /** Not asked yet. */
    data object Hidden : ParentGateState

    /** Showing "What is 7 + 5?". */
    data class Awaiting(
        val question: GenerateParentGateUseCase.Question,
        val entered: String,
        val wasWrong: Boolean,
    ) : ParentGateState

    /** Answered correctly. */
    data object Passed : ParentGateState
}

data class ParentZoneUiState(
    val gate: ParentGateState = ParentGateState.Hidden,
    val settings: AppSettings = AppSettings.DEFAULT,
    val dashboard: DashboardSnapshot = DashboardSnapshot(),
    val weekly: WeeklyActivity = WeeklyActivity.empty(),
    val resetDone: Boolean = false,
)

/**
 * The parent zone.
 *
 * ## The maths gate
 * `7 + 5` is not security - a determined seven-year-old can work it out. It is a
 * *speed bump* that raises the cost of wandering into a settings screen far enough
 * that a child will not do it by accident, while a parent gets through in two
 * seconds. That is the right trade for an app with no accounts, no purchases and
 * no ads: there is nothing behind the door worth attacking.
 *
 * The question is regenerated on every failed attempt, so a child cannot brute-force
 * it by remembering a previous sum.
 */
@HiltViewModel
class ParentZoneViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    statsRepository: StatsRepository,
    private val generateGate: GenerateParentGateUseCase,
    private val resetProgressUseCase: ResetProgressUseCase,
) : ViewModel() {

    private val gate = MutableStateFlow<ParentGateState>(ParentGateState.Hidden)
    private val entered = MutableStateFlow("")
    private val wasWrong = MutableStateFlow(false)
    private val resetDone = MutableStateFlow(false)

    val uiState: StateFlow<ParentZoneUiState> = combine(
        settingsRepository.settings,
        statsRepository.observeDashboard(),
        statsRepository.observeWeeklyActivity(java.time.LocalDate.now()),
        gate,
        resetDone,
    ) { settings, dashboard, weekly, gateState, done ->
        ParentZoneUiState(
            gate = if (gateState is ParentGateState.Awaiting) {
                gateState.copy(entered = entered.value, wasWrong = wasWrong.value)
            } else {
                gateState
            },
            settings = settings,
            dashboard = dashboard,
            weekly = weekly,
            resetDone = done,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ParentZoneUiState(),
    )

    // ------------------------------------------------------------------- gate

    fun requireGate() {
        if (_passed) return
        entered.value = ""
        wasWrong.value = false
        // A fresh seed per challenge: the use case defaults to one question per
        // day, which would let a child memorise this morning's sum and walk
        // through the gate all afternoon.
        gate.value = ParentGateState.Awaiting(generateGate(System.nanoTime()), "", false)
    }

    /** Leaves the gate, e.g. when the parent taps Cancel. */
    fun dismissGate() {
        if (_passed) return
        gate.value = ParentGateState.Hidden
    }

    fun onAnswerChanged(value: String) {
        entered.value = value.filter { it.isDigit() }.take(MAX_ANSWER_LENGTH)
        if (wasWrong.value) wasWrong.value = false
    }

    fun submitGateAnswer() {
        val current = gate.value as? ParentGateState.Awaiting ?: return
        val answer = entered.value.trim().toIntOrNull()
        if (answer == current.question.answer) {
            _passed = true
            gate.value = ParentGateState.Passed
        } else {
            // A fresh question each time defeats memorisation.
            wasWrong.value = true
            entered.value = ""
            gate.value = ParentGateState.Awaiting(generateGate(System.nanoTime()), "", true)
        }
    }

    /**
     * Latched for the life of the ViewModel.
     *
     * The ViewModel is scoped to the parent-zone nav entry, so the gate is asked
     * once per visit rather than once per interaction. Holding it in the
     * ViewModel rather than in a singleton is what makes it expire when the parent
     * leaves the screen - a child navigating back must be challenged again.
     */
    private var _passed: Boolean = false

    // --------------------------------------------------------------- settings

    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSoundEnabled(enabled)
    }

    fun setLanguage(language: UiLanguage) = viewModelScope.launch {
        settingsRepository.setUiLanguage(language)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setChildName(name: String) = viewModelScope.launch {
        settingsRepository.setChildName(name)
    }

    fun setParentGateEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setParentGateEnabled(enabled)
    }

    fun setDailyGoal(goal: Int) = viewModelScope.launch {
        settingsRepository.setDailyGoal(goal)
    }

    /**
     * Clears all learning history. The gate is the confirmation.
     *
     * Named distinctly from the injected use case so the launch body cannot
     * resolve `resetProgress()` to itself.
     */
    fun onResetProgress() = viewModelScope.launch {
        resetProgressUseCase()
        resetDone.value = true
    }

    fun acknowledgeReset() {
        resetDone.value = false
    }

    private companion object {
        const val MAX_ANSWER_LENGTH = 2
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
