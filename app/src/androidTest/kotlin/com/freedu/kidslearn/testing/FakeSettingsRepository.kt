package com.freedu.kidslearn.testing

import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.model.UiLanguage
import com.freedu.kidslearn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory settings for instrumented tests, so no DataStore file is written. */
class FakeSettingsRepository : SettingsRepository {

    private val state = MutableStateFlow(AppSettings.DEFAULT)

    override val settings: Flow<AppSettings> = state

    override suspend fun setSoundEnabled(enabled: Boolean) {
        state.value = state.value.copy(soundEnabled = enabled)
    }

    override suspend fun setUiLanguage(language: UiLanguage) {
        state.value = state.value.copy(uiLanguage = language)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setChildName(name: String) {
        state.value = state.value.copy(childName = name)
    }

    override suspend fun setLastModule(module: ModuleType?) {
        state.value = state.value.copy(lastModule = module)
    }

    override suspend fun setParentGateEnabled(enabled: Boolean) {
        state.value = state.value.copy(parentGateEnabled = enabled)
    }

    /** Lets a test start from a non-default state, e.g. a child already in the app. */
    fun seed(settings: AppSettings) {
        state.value = settings
    }
}
