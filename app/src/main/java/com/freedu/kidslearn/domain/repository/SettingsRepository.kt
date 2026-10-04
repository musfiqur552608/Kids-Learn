package com.freedu.kidslearn.domain.repository

import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.model.UiLanguage
import kotlinx.coroutines.flow.Flow

/** User preferences, backed by Preferences DataStore. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setSoundEnabled(enabled: Boolean)

    suspend fun setUiLanguage(language: UiLanguage)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setChildName(name: String)

    suspend fun setLastModule(module: ModuleType?)

    suspend fun setParentGateEnabled(enabled: Boolean)

    /** Coerced to [AppSettings.MIN_DAILY_GOAL]..[AppSettings.MAX_DAILY_GOAL]. */
    suspend fun setDailyGoal(goal: Int)

    /** Records that today's goal celebration already fired. */
    suspend fun markGoalCelebrated(today: java.time.LocalDate)
}
