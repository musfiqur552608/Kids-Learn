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

    suspend fun setMusicEnabled(enabled: Boolean)

    suspend fun setUiLanguage(language: UiLanguage)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setChildName(name: String)

    suspend fun setLastModule(module: ModuleType?)

    suspend fun setParentGateEnabled(enabled: Boolean)
}
