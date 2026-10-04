package com.freedu.kidslearn.core.audio

import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.model.UiLanguage
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pins the bundled-first routing: a line with a clip plays the clip and never
 * touches system TTS; anything else delegates. Either path stays silent when
 * sound is off or the text is blank.
 */
class BundledSpeechPlayerTest {

    private class FakeSettings(initial: AppSettings = AppSettings.DEFAULT) : SettingsRepository {
        var current = initial
        val flow = MutableStateFlow(initial)
        override val settings = flow
        private fun update(next: AppSettings) {
            current = next
            flow.value = next
        }
        override suspend fun setSoundEnabled(enabled: Boolean) {
            update(current.copy(soundEnabled = enabled))
        }
        override suspend fun setUiLanguage(language: UiLanguage) {
            update(current.copy(uiLanguage = language))
        }
        override suspend fun setThemeMode(mode: ThemeMode) {
            update(current.copy(themeMode = mode))
        }
        override suspend fun setChildName(name: String) {
            update(current.copy(childName = name))
        }
        override suspend fun setLastModule(module: ModuleType?) {
            update(current.copy(lastModule = module))
        }
        override suspend fun setParentGateEnabled(enabled: Boolean) {
            update(current.copy(parentGateEnabled = enabled))
        }
        override suspend fun setDailyGoal(goal: Int) {
            update(
                current.copy(
                    dailyGoal = goal.coerceIn(AppSettings.MIN_DAILY_GOAL, AppSettings.MAX_DAILY_GOAL),
                ),
            )
        }
        override suspend fun markGoalCelebrated(today: java.time.LocalDate) {
            update(current.copy(goalCelebratedDate = today))
        }
    }

    private class FakeManifest(private val files: Map<String, String>) : NarrationManifest {
        override fun fileFor(text: String, localeTag: String) =
            files["$localeTag\n$text"]
    }

    private class FakeClips(var playable: Boolean = true) : NarrationClipPlayer {
        val played = mutableListOf<String>()
        var stops = 0
        override fun play(assetPath: String): Boolean {
            if (!playable) return false
            played += assetPath
            return true
        }
        override fun stop() {
            stops++
        }
    }

    private class FakeTts : SpeechPlayer {
        val spoken = mutableListOf<Pair<String, String>>()
        override fun speak(text: String, localeTag: String, flush: Boolean) {
            spoken += text to localeTag
        }
    }

    // NOTE: the player is given backgroundScope, not the test body scope: its
    // settings collector never completes, and a never-completing child of the
    // body scope fails the test with UncompletedCoroutinesError.
    private fun TestScope.player(
        files: Map<String, String> = mapOf("bn-BD\nখ, খরগোশ" to "bn_l02.mp3"),
        settings: FakeSettings = FakeSettings(),
        clips: FakeClips = FakeClips(),
        tts: FakeTts = FakeTts(),
    ) = BundledSpeechPlayer(
        manifest = FakeManifest(files),
        clips = clips,
        systemTts = tts,
        settingsRepository = settings,
        applicationScope = backgroundScope,
    )

    @Test
    fun `bundled line plays the clip and skips system tts`() = runTest {
        val settings = FakeSettings()
        val clips = FakeClips()
        val tts = FakeTts()
        val p = player(settings = settings, clips = clips, tts = tts)
        runCurrent()

        p.speak("খ, খরগোশ", "bn-BD")

        assertThat(clips.played).containsExactly("narration/bn_l02.mp3")
        assertThat(tts.spoken).isEmpty()
    }

    @Test
    fun `unbundled line delegates to system tts`() = runTest {
        val clips = FakeClips()
        val tts = FakeTts()
        val p = player(clips = clips, tts = tts)
        runCurrent()

        p.speak("3 plus 2", "en-US")

        assertThat(clips.played).isEmpty()
        assertThat(tts.spoken).containsExactly("3 plus 2" to "en-US")
    }

    @Test
    fun `unplayable clip falls back to system tts`() = runTest {
        val clips = FakeClips(playable = false)
        val tts = FakeTts()
        val p = player(clips = clips, tts = tts)
        runCurrent()

        p.speak("খ, খরগোশ", "bn-BD")

        assertThat(tts.spoken).containsExactly("খ, খরগোশ" to "bn-BD")
    }

    @Test
    fun `muted speak plays nothing`() = runTest {
        val settings = FakeSettings(AppSettings.DEFAULT.copy(soundEnabled = false))
        val clips = FakeClips()
        val tts = FakeTts()
        val p = player(settings = settings, clips = clips, tts = tts)
        runCurrent()

        p.speak("খ, খরগোশ", "bn-BD")
        p.speak("3 plus 2", "en-US")

        assertThat(clips.played).isEmpty()
        assertThat(tts.spoken).isEmpty()
    }

    @Test
    fun `blank text is ignored`() = runTest {
        val clips = FakeClips()
        val tts = FakeTts()
        val p = player(clips = clips, tts = tts)
        runCurrent()

        p.speak("   ", "bn-BD")

        assertThat(clips.played).isEmpty()
        assertThat(tts.spoken).isEmpty()
    }
}
