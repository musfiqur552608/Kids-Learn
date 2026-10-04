package com.freedu.kidslearn.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.model.UiLanguage
import com.freedu.kidslearn.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File name for the preferences DataStore.
 *
 * Declared as a top-level constant because the `by preferencesDataStore(...)`
 * delegate below is initialised at class-load time, *before* any class body -
 * including a companion object - is available to it.
 */
private const val SETTINGS_FILE = "kids_learn_settings"

/** Child name is truncated so a parent's typo cannot blow up the layout. */
private const val MAX_CHILD_NAME_LENGTH = 24

/**
 * Preferences DataStore instance.
 *
 * Declared as a top-level extension property so the delegate is created exactly
 * once per process. Constructing a second `DataStore` for the same file throws at
 * runtime, and that mistake is invisible until the app crashes in front of a
 * parent - the extension property is the only construction pattern that makes the
 * bug impossible to write.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_FILE,
)

/**
 * DataStore-backed settings.
 *
 * ## Why DataStore and not Room
 * These are a handful of single scalar values, written a few times per session and
 * read on every screen. DataStore's `Flow` API makes them reactive for free, and it
 * keeps preferences out of the game database - so "reset all progress" can wipe
 * learning history without silently discarding a parent's settings.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private val dataStore: DataStore<Preferences> get() = context.settingsDataStore

    /**
     * `catch` on [IOException] is mandatory, not defensive: a corrupt or
     * unreadable preferences file would otherwise propagate the exception and
     * cancel every collector - which, since this Flow is read by every screen,
     * means the entire app. Falling back to empty preferences loses the child's
     * choices but keeps the app usable.
     */
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    override val settings: Flow<AppSettings> = preferences
        .map { prefs ->
            AppSettings(
                uiLanguage = UiLanguage.fromTag(prefs[Keys.UI_LANGUAGE]),
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                childName = prefs[Keys.CHILD_NAME].orEmpty(),
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.LIGHT,
                lastModule = prefs[Keys.LAST_MODULE]
                    ?.let { name -> ModuleType.entries.firstOrNull { it.name == name } },
                parentGateEnabled = prefs[Keys.PARENT_GATE] ?: true,
                dailyGoal = (prefs[Keys.DAILY_GOAL] ?: AppSettings.DEFAULT_DAILY_GOAL)
                    .coerceIn(AppSettings.MIN_DAILY_GOAL, AppSettings.MAX_DAILY_GOAL),
                goalCelebratedDate = prefs[Keys.GOAL_CELEBRATED_DATE]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
            )
        }
        // Every screen reads settings; without this, an unrelated write would
        // re-emit an identical value and recompose the whole tree.
        .distinctUntilChanged()

    override suspend fun setSoundEnabled(enabled: Boolean) = put(Keys.SOUND_ENABLED, enabled)

    override suspend fun setUiLanguage(language: UiLanguage) = put(Keys.UI_LANGUAGE, language.tag)

    override suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME_MODE, mode.name)

    override suspend fun setChildName(name: String) =
        put(Keys.CHILD_NAME, name.take(MAX_CHILD_NAME_LENGTH))

    override suspend fun setParentGateEnabled(enabled: Boolean) = put(Keys.PARENT_GATE, enabled)

    override suspend fun setDailyGoal(goal: Int) = put(
        Keys.DAILY_GOAL,
        goal.coerceIn(AppSettings.MIN_DAILY_GOAL, AppSettings.MAX_DAILY_GOAL),
    )

    override suspend fun markGoalCelebrated(today: java.time.LocalDate) =
        put(Keys.GOAL_CELEBRATED_DATE, today.toString())

    /**
     * `null` clears the key.
     *
     * `Preferences.Key<String>` is non-nullable, so a null value cannot simply be
     * assigned - `remove()` is the correct way to express "no value", and it is
     * what makes "Continue learning" disappear once the child picks a new module.
     */
    override suspend fun setLastModule(module: ModuleType?) {
        val name = module?.name
        dataStore.edit { prefs ->
            if (name == null) prefs.remove(Keys.LAST_MODULE) else prefs[Keys.LAST_MODULE] = name
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    private object Keys {
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val CHILD_NAME = stringPreferencesKey("child_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_MODULE = stringPreferencesKey("last_module")
        val PARENT_GATE = booleanPreferencesKey("parent_gate_enabled")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val GOAL_CELEBRATED_DATE = stringPreferencesKey("goal_celebrated_date")
    }
}
