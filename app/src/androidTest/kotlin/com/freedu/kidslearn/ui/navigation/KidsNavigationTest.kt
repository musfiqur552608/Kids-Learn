package com.freedu.kidslearn.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.ui.theme.KidsLearnTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.freedu.kidslearn.data.content.ContentRepositoryImpl
import com.freedu.kidslearn.data.content.QuizFactory
import com.freedu.kidslearn.testing.FakeProgressRepository
import com.freedu.kidslearn.testing.FakeSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Compose navigation test with the real content catalog and faked persistence.
 *
 * ## What this actually verifies
 * The thing most likely to break in a refactor of this app is the *route wiring*:
 * a pattern that no longer matches the one `createRoute` builds produces a blank
 * screen, which is invisible in a unit test. Driving the real [KidsNavHost] and
 * asserting that a destination's heading appears is the only way to catch that.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KidsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Inject
    @Singleton
    lateinit var contentRepository: ContentRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun homeShowsEveryModuleTile() {
        setContent(startDestination = Route.Home)

        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("Bangla").assertIsDisplayed()
        composeRule.onNodeWithText("Arabic").assertIsDisplayed()
        composeRule.onNodeWithText("Maths").assertIsDisplayed()
    }

    @Test
    fun tappingEnglishOpensTheEnglishModule() {
        setContent(startDestination = Route.Home)

        composeRule.onNodeWithText("English").performClick()
        composeRule.waitForIdle()

        // The module overview greets with the subject name and offers its letters.
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("All letters").assertIsDisplayed()
    }

    @Test
    fun openingTheLetterListShowsTheFirstLetter() {
        setContent(startDestination = Route.AlphabetModule.createRoute(ModuleType.ENGLISH))

        composeRule.onNodeWithText("All letters").performClick()
        composeRule.waitForIdle()

        // A, B, C, D ... - the letter glyph is the content of the card.
        composeRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun theGamesHubListsAllThreeGames() {
        setContent(startDestination = Route.GamesHub)

        composeRule.onNodeWithText("Memory match").assertIsDisplayed()
        composeRule.onNodeWithText("Find it").assertIsDisplayed()
        composeRule.onNodeWithText("Quick quiz").assertIsDisplayed()
    }

    @Test
    fun theProgressScreenRendersWithNoHistory() {
        setContent(startDestination = Route.Progress)

        composeRule.onNodeWithText("Progress").assertIsDisplayed()
    }

    @Test
    fun theParentZoneAsksTheGateQuestion() {
        setContent(startDestination = Route.ParentZone)

        // The gate is what keeps a child out; it must appear immediately.
        composeRule.onNodeWithText("Grown-ups only").assertIsDisplayed()
    }

    private fun setContent(startDestination: Route) {
        composeRule.setContent {
            KidsLearnTheme(useDarkTheme = false) {
                KidsNavHost(startDestination = startDestination)
            }
        }
    }
}

/**
 * Test-only Hilt module replacing the Room-backed repositories.
 *
 * Installed with `@TestInstallIn`, which *replaces* the production
 * `RepositoryModule` rather than adding to it - that is what makes the fake the
 * only binding in the graph.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class])
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideContentRepository(): ContentRepository = ContentRepositoryImpl(QuizFactory())

    @Provides
    @Singleton
    fun provideProgressRepository(): ProgressRepository = FakeProgressRepository()

    @Provides
    @Singleton
    fun provideStatsRepository(): StatsRepository = FakeProgressRepository()

    @Provides
    @Singleton
    fun provideSettingsRepository(): SettingsRepository = FakeSettingsRepository()
}
