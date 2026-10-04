package com.freedu.kidslearn.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.freedu.kidslearn.domain.model.AnswerOption
import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.DashboardSnapshot
import com.freedu.kidslearn.domain.model.DayActivity
import com.freedu.kidslearn.domain.model.GameResult
import com.freedu.kidslearn.domain.model.WeeklyActivity
import com.freedu.kidslearn.domain.model.GameScore
import com.freedu.kidslearn.domain.model.GameType
import com.freedu.kidslearn.domain.model.ModuleProgress
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizKind
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.domain.model.QuizResult
import com.freedu.kidslearn.domain.model.UserStats
import com.freedu.kidslearn.domain.usecase.GenerateParentGateUseCase
import com.freedu.kidslearn.ui.alphabet.AlphabetModuleScreen
import com.freedu.kidslearn.ui.alphabet.AlphabetStage
import com.freedu.kidslearn.ui.alphabet.AlphabetUiState
import com.freedu.kidslearn.ui.games.FindCorrectScreen
import com.freedu.kidslearn.ui.games.FindCorrectUiState
import com.freedu.kidslearn.ui.games.GameCardUi
import com.freedu.kidslearn.ui.quiz.GameResultScreen
import com.freedu.kidslearn.ui.games.GamesHubScreen
import com.freedu.kidslearn.ui.games.GamesHubUiState
import com.freedu.kidslearn.ui.games.MemoryMatchScreen
import com.freedu.kidslearn.ui.games.MemoryUiState
import com.freedu.kidslearn.ui.games.OddOneOutScreen
import com.freedu.kidslearn.ui.games.OddOneOutUiState
import com.freedu.kidslearn.ui.games.TimedQuizScreen
import com.freedu.kidslearn.ui.games.TimedQuizUiState
import com.freedu.kidslearn.ui.home.HomeScreen
import com.freedu.kidslearn.ui.home.HomeUiState
import com.freedu.kidslearn.ui.home.ModuleTileUi
import com.freedu.kidslearn.ui.maths.MathsScreen
import com.freedu.kidslearn.ui.maths.MathsUiState
import com.freedu.kidslearn.ui.parentzone.ParentGateState
import com.freedu.kidslearn.ui.parentzone.ParentZoneScreen
import com.freedu.kidslearn.ui.parentzone.ParentZoneUiState
import com.freedu.kidslearn.ui.progress.ProgressScreen
import com.freedu.kidslearn.ui.quiz.QuizResultScreen
import com.freedu.kidslearn.ui.quiz.QuizScreen
import com.freedu.kidslearn.ui.quiz.QuizStage
import com.freedu.kidslearn.ui.quiz.QuizUiState
import com.freedu.kidslearn.ui.theme.KidsLearnTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders every top-level screen under Robolectric.
 *
 * ## Why this test exists
 * Two resource-reference bugs reached a real device before either could have been
 * caught by a unit test: an argument passed twice to `pluralStringResource`, and
 * a `<plurals>` id passed to `stringResource` - which resolves plain strings only,
 * and throws `Resources$NotFoundException: String resource ID ... is a complex map
 * type` the moment the screen composes. Kotlin type-checks both, lint does not
 * flag them, and no domain test ever executes a composable, so both failures
 * appeared for the first time on a child's phone - taking the whole app down on
 * the first screen they opened.
 *
 * Rendering each screen with representative state turns that class of bug into a
 * build failure. It also catches the cheaper mistakes: a `when` branch that lost
 * its default, a divide-by-zero on an empty state, or a composable reading a
 * resource the app does not ship.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenRenderingTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(content: @Composable () -> Unit) {
        compose.setContent {
            KidsLearnTheme { content() }
        }
        compose.waitForIdle()
    }

    // --- fixtures -------------------------------------------------------------

    private val question = QuizQuestion(
        id = "ENGLISH_A",
        moduleType = ModuleType.ENGLISH,
        kind = QuizKind.LETTER_TO_PICTURE,
        promptVisual = "A",
        options = listOf(
            AnswerOption(id = "a", label = "Apple", visual = "🍎"),
            AnswerOption(id = "b", label = "Ball", visual = "⚽"),
            AnswerOption(id = "c", label = "Cat", visual = "🐱"),
        ),
        correctIndex = 0,
        speakText = "A, A for Apple",
        speakLocale = "en-US",
        subjectItemId = "ENGLISH_A",
    )

    private fun dashboard(
        completed: Int = 13,
        total: Int = 26,
        badges: List<Badge> = listOf(Badge(BadgeKey.FIRST_STEP, LocalDate.now())),
    ) = DashboardSnapshot(
        stats = UserStats(
            totalStars = 14,
            totalCoins = 20,
            currentStreak = 3,
            longestStreak = 5,
            lessonsCompleted = 4,
            gamesPlayed = 2,
        ),
        modules = listOf(
            ModuleProgress(ModuleType.ENGLISH, total, completed, totalStars = 9, maxStars = total * 3),
            ModuleProgress(ModuleType.MATHS, 33, 0, totalStars = 0, maxStars = 99),
        ),
        badges = badges,
        recentScores = listOf(GameScore(GameType.MEMORY_MATCH, 9, 2, LocalDate.now())),
        totalItems = total + 33,
    )

    // --- screens --------------------------------------------------------------

    @Test
    fun homeScreenRendersWithModuleProgress() {
        render {
            HomeScreen(
                state = HomeUiState(
                    childName = "Mitu",
                    lastModule = ModuleType.ENGLISH,
                    tiles = listOf(
                        ModuleTileUi(ModuleType.ENGLISH, "abc", 13, 26),
                        ModuleTileUi(ModuleType.BANGLA, "অ", 4, 46),
                        ModuleTileUi(ModuleType.ARABIC, "ب", 0, 28),
                        ModuleTileUi(ModuleType.MATHS, "➕", 7, 33),
                    ),
                    totalStars = 14,
                    totalCoins = 20,
                    currentStreak = 3,
                    gamesPlayed = 2,
                ),
                onOpenModule = {},
                onOpenGames = {},
                onOpenProgress = {},
                onOpenParentZone = {},
            )
        }
        // Built from R.plurals.cd_module_progress: "%1$s: %2$d of %3$d complete".
        compose.onNode(hasContentDescription("English: 13 of 26 complete")).assertExists()
    }

    @Test
    fun homeScreenRendersEmptyState() {
        // Day one, nothing learned yet: the state where "0 of 0" formatting hides.
        render {
            HomeScreen(
                state = HomeUiState(),
                onOpenModule = {},
                onOpenGames = {},
                onOpenProgress = {},
                onOpenParentZone = {},
            )
        }
    }

    @Test
    fun alphabetOverviewRendersLetterCount() {
        render {
            AlphabetModuleScreen(
                state = AlphabetUiState(
                    moduleType = ModuleType.ENGLISH,
                    stage = AlphabetStage.OVERVIEW,
                    totalItems = 26,
                    completedItems = 13,
                    totalStars = 20,
                ),
                onBack = {},
                onShowOverview = {},
                onShowLetterList = {},
                onSelectLetter = {},
                onClearSelection = {},
                onPronounce = {},
                onTakeQuiz = {},
                onTraceComplete = {},
            )
        }
        // R.plurals.letters_count - the call that shipped a <plurals> id to
        // stringResource() and crashed on the first screen a child reached.
        compose.onNode(hasText("13 of 26 learned")).assertExists()
    }

    @Test
    fun alphabetOverviewRendersArabicEmptyState() {
        render {
            AlphabetModuleScreen(
                state = AlphabetUiState(
                    moduleType = ModuleType.ARABIC,
                    stage = AlphabetStage.OVERVIEW,
                ),
                onBack = {},
                onShowOverview = {},
                onShowLetterList = {},
                onSelectLetter = {},
                onClearSelection = {},
                onPronounce = {},
                onTakeQuiz = {},
                onTraceComplete = {},
            )
        }
        compose.onNode(hasText("0 of 0 learned")).assertExists()
    }

    @Test
    fun progressScreenRenders() {
        render { ProgressScreen(state = dashboard(), onBack = {}) }

        // R.plurals.progress_percent, near the top of the list.
        assertThat(
            compose.onAllNodes(hasText("%", substring = true)).fetchSemanticsNodes(),
        ).isNotEmpty()
    }

    @Test
    fun progressScreenRendersBadgeTally() {
        // Modules are omitted so the badge heading lands inside the first
        // screenful: a LazyColumn never composes - and therefore never formats -
        // an item the child would have to scroll to.
        render {
            ProgressScreen(
                state = DashboardSnapshot(
                    stats = UserStats(totalStars = 1),
                    modules = emptyList(),
                    badges = listOf(Badge(BadgeKey.FIRST_STEP, LocalDate.now())),
                ),
                onBack = {},
            )
        }
        // R.plurals.progress_badges_earned.
        compose.onNode(
            hasText("1 of ${BadgeKey.entries.size} earned", substring = true),
        ).assertExists()
    }

    @Test
    fun progressScreenRendersEmptySnapshot() {
        render { ProgressScreen(state = DashboardSnapshot(), onBack = {}) }
    }

    @Test
    fun parentZoneRendersWithGateAndPercentages() {
        render {
            ParentZoneScreen(
                state = ParentZoneUiState(
                    gate = ParentGateState.Awaiting(
                        question = GenerateParentGateUseCase.Question(7, 5, 12),
                        entered = "",
                        wasWrong = false,
                    ),
                    settings = AppSettings.DEFAULT,
                    dashboard = dashboard(),
                ),
                onBack = {},
                onRequireGate = {},
                onDismissGate = {},
                onAnswerChanged = {},
                onSubmitAnswer = {},
                onSetSound = {},
                onSetLanguage = {},
                onSetTheme = {},
                onSetChildName = {},
                onSetParentGate = {},
                onResetProgress = {},
                onAcknowledgeReset = {},
            )
        }
        // R.plurals.parent_gate_question.
        compose.onNode(hasText("7 + 5", substring = true)).assertExists()
    }

    @Test
    fun parentZoneRendersWeeklyChart() {
        val today = LocalDate.now()
        render {
            ParentZoneScreen(
                state = ParentZoneUiState(
                    gate = ParentGateState.Passed,
                    settings = AppSettings.DEFAULT,
                    dashboard = dashboard(),
                    weekly = WeeklyActivity(
                        (6 downTo 0).map { back ->
                            DayActivity(today.minusDays(back.toLong()), lessons = back, games = 1)
                        },
                    ),
                ),
                onBack = {},
                onRequireGate = {},
                onDismissGate = {},
                onAnswerChanged = {},
                onSubmitAnswer = {},
                onSetSound = {},
                onSetLanguage = {},
                onSetTheme = {},
                onSetChildName = {},
                onSetParentGate = {},
                onResetProgress = {},
                onAcknowledgeReset = {},
            )
        }
        compose.onNode(hasText("This week", substring = true)).assertExists()
    }

    @Test
    fun mathsScreenRenders() {
        render {
            MathsScreen(
                state = MathsUiState(isLoading = false, totalItems = 33, completedItems = 7),
                onBack = {},
                onSelectTab = {},
                onSelectNumber = {},
                onClearSelection = {},
                onTakeQuiz = {},
                onPronounce = {},
                onPronounceShape = {},
            )
        }
    }

    @Test
    fun gamesHubRenders() {
        render {
            GamesHubScreen(
                state = GamesHubUiState(
                    games = listOf(
                        GameCardUi(GameType.MEMORY_MATCH, bestScore = 9),
                        GameCardUi(GameType.FIND_THE_CORRECT_ONE, bestScore = 0),
                        GameCardUi(GameType.TIMED_QUIZ, bestScore = 4),
                        GameCardUi(GameType.ODD_ONE_OUT, bestScore = 0),
                    ),
                    isLoading = false,
                ),
                onBack = {},
                onOpenGame = {},
            )
        }
    }

    @Test
    fun memoryMatchRenders() {
        render {
            MemoryMatchScreen(
                state = MemoryUiState(taps = 7, bestScore = 9),
                onBack = {},
                onCardTapped = {},
            )
        }
    }

    @Test
    fun findCorrectRendersRoundProgress() {
        render {
            FindCorrectScreen(
                state = FindCorrectUiState(round = 2, totalRounds = 8, correctAnswers = 3),
                onBack = {},
                onOptionTapped = {},
            )
        }
    }

    @Test
    fun oddOneOutRendersRoundProgress() {
        render {
            OddOneOutScreen(
                state = OddOneOutUiState(
                    options = listOf("A", "A", "A", "B"),
                    oddIndex = 3,
                    round = 2,
                    totalRounds = 8,
                    correctAnswers = 3,
                ),
                onBack = {},
                onOptionTapped = {},
            )
        }
        compose.onNode(hasText("Odd one out", substring = true)).assertExists()
    }

    @Test
    fun timedQuizRendersScore() {
        render {
            TimedQuizScreen(
                state = TimedQuizUiState(question = question, score = 5, questionNumber = 2),
                onBack = {},
                onAnswerSelected = {},
            )
        }
    }

    @Test
    fun quizScreenRendersQuestionProgress() {
        render {
            QuizScreen(
                state = QuizUiState(
                    questions = listOf(question),
                    currentIndex = 0,
                    stage = QuizStage.ACTIVE,
                    moduleType = ModuleType.ENGLISH,
                ),
                onAnswer = {},
                onNext = {},
                onReplayPrompt = {},
                onBack = {},
            )
        }
        // R.plurals.quiz_question_of.
        compose.onNode(hasText("Question 1 of 1")).assertExists()
    }

    @Test
    fun quizResultRendersStarCount() {
        render {
            QuizResultScreen(
                result = QuizResult(
                    moduleType = ModuleType.ENGLISH,
                    totalQuestions = 5,
                    correctAnswers = 5,
                    starsEarned = 3,
                    coinsEarned = 15,
                    itemIds = listOf("A", "B", "C", "D", "E"),
                    newBadges = listOf(BadgeKey.FIRST_STEP),
                ),
                showCelebration = false,
                onPlayAgain = {},
                onGoHome = {},
                onDismissCelebration = {},
            )
        }
        // R.plurals.quiz_correct_count, plural form "other" -> "5 out of 5".
        // `result_stars_earned` is also resolved here: it is an argument of the
        // celebration overlay, so it formats during composition even while hidden.
        compose.onNode(hasText("5 out of 5")).assertExists()
        // The newly unlocked badge is announced inline on the result screen.
        compose.onNode(hasText("First step", substring = true)).assertExists()
    }

    @Test
    fun quizResultRendersSingleStar() {
        render {
            QuizResultScreen(
                result = QuizResult(
                    moduleType = ModuleType.BANGLA,
                    totalQuestions = 5,
                    correctAnswers = 1,
                    starsEarned = 1,
                    coinsEarned = 3,
                    itemIds = listOf("ক"),
                ),
                showCelebration = false,
                onPlayAgain = {},
                onGoHome = {},
                onDismissCelebration = {},
            )
        }
        // Same plural, form "one" -> "1 out of 5"; `result_stars_earned` takes its
        // "one" branch here ("You earned 1 star"), which is where a bad argument
        // order would blow up.
        compose.onNode(hasText("1 out of 5")).assertExists()
    }

    @Test
    fun quizResultRendersPersonalBest() {
        render {
            QuizResultScreen(
                result = QuizResult(
                    moduleType = ModuleType.ENGLISH,
                    totalQuestions = 5,
                    correctAnswers = 4,
                    starsEarned = 2,
                    coinsEarned = 10,
                    itemIds = listOf("A", "B", "C", "D"),
                    isNewPersonalBest = true,
                ),
                showCelebration = false,
                onPlayAgain = {},
                onGoHome = {},
                onDismissCelebration = {},
            )
        }
        // Shown on the game result screen too; the quiz used to omit it.
        compose.onNode(hasText("New best!", substring = true)).assertExists()
    }

    @Test
    fun gameResultRenders() {
        render {
            GameResultScreen(
                result = GameResult(
                    gameType = GameType.MEMORY_MATCH,
                    score = 12,
                    totalRounds = 8,
                    correctAnswers = 7,
                    starsEarned = 2,
                    coinsEarned = 10,
                ),
                showCelebration = false,
                onPlayAgain = {},
                onGoHome = {},
                onDismissCelebration = {},
            )
        }
    }
}
