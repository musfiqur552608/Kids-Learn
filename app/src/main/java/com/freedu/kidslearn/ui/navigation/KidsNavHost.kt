package com.freedu.kidslearn.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.arabic.ArabicModuleScreen
import com.freedu.kidslearn.ui.bangla.BanglaModuleScreen
import com.freedu.kidslearn.ui.english.EnglishModuleScreen
import com.freedu.kidslearn.ui.games.FindCorrectScreen
import com.freedu.kidslearn.ui.games.FindCorrectViewModel
import com.freedu.kidslearn.ui.games.GamesHubScreen
import com.freedu.kidslearn.ui.games.GamesHubViewModel
import com.freedu.kidslearn.ui.games.MemoryMatchScreen
import com.freedu.kidslearn.ui.games.MemoryMatchViewModel
import com.freedu.kidslearn.ui.games.TimedQuizScreen
import com.freedu.kidslearn.ui.games.TimedQuizViewModel
import com.freedu.kidslearn.ui.home.HomeScreen
import com.freedu.kidslearn.ui.home.HomeViewModel
import com.freedu.kidslearn.ui.maths.MathsScreen
import com.freedu.kidslearn.ui.maths.MathsViewModel
import com.freedu.kidslearn.ui.parentzone.ParentZoneScreen
import com.freedu.kidslearn.ui.parentzone.ParentZoneViewModel
import com.freedu.kidslearn.ui.progress.ProgressScreen
import com.freedu.kidslearn.ui.progress.ProgressViewModel
import com.freedu.kidslearn.ui.quiz.QuizScreen
import com.freedu.kidslearn.ui.quiz.QuizViewModel
import com.freedu.kidslearn.ui.splash.SplashScreen

/**
 * The app's navigation graph.
 *
 * ## Why one shared quiz destination
 * `Route.Quiz` describes a quiz entirely by its arguments, so English, Bangla,
 * Arabic, maths and the games all land on the *same* destination and the same
 * ViewModel. A per-module quiz route would multiply the graph by five and make the
 * back stack harder to reason about, for no behavioural difference.
 *
 * ## Argument parsing
 * Route arguments arrive as strings because nav routes are strings. Each argument
 * is parsed in one place - either here or in the destination's ViewModel, via
 * `SavedStateHandle` - with a safe fallback, so a malformed deep link degrades to a
 * default rather than crashing.
 */
@Composable
fun KidsNavHost(
    startDestination: Route,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.pattern,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Route.Splash.pattern) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Route.Home.pattern) {
                        // The splash must not be on the back stack: pressing back
                        // from Home should leave the app, not replay the intro.
                        popUpTo(Route.Splash.pattern) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Home.pattern) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onOpenModule = { module -> navController.navigate(routeForModule(module)) },
                onOpenGames = { navController.navigate(Route.GamesHub.pattern) },
                onOpenProgress = { navController.navigate(Route.Progress.pattern) },
                onOpenParentZone = { navController.navigate(Route.ParentZone.pattern) },
            )
        }

        // --- Alphabet modules --------------------------------------------------

        composable(
            route = Route.AlphabetModule.pattern,
            arguments = listOf(
                navArgument(Route.AlphabetModule.ARG_MODULE) {
                    type = NavType.StringType
                },
            ),
        ) { entry ->
            // The argument decides which ViewModel is requested, so the three
            // alphabet screens share one route *and* one composable body.
            when (entry.argumentModule()) {
                ModuleType.BANGLA -> BanglaModuleScreen(
                    onBack = { navController.popBackStack() },
                    onTakeQuiz = { module, itemId -> navController.navigate(quizRoute(module, itemId)) },
                )

                ModuleType.ARABIC -> ArabicModuleScreen(
                    onBack = { navController.popBackStack() },
                    onTakeQuiz = { module, itemId -> navController.navigate(quizRoute(module, itemId)) },
                )

                else -> EnglishModuleScreen(
                    onBack = { navController.popBackStack() },
                    onTakeQuiz = { module, itemId -> navController.navigate(quizRoute(module, itemId)) },
                )
            }
        }

        // --- Maths --------------------------------------------------------------

        composable(Route.MathsModule.pattern) {
            val viewModel: MathsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MathsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSelectTab = viewModel::selectTab,
                onSelectNumber = viewModel::selectNumber,
                onClearSelection = viewModel::clearSelection,
                onTakeQuiz = { _ -> navController.navigate(Route.Quiz.createRoute(ModuleType.MATHS)) },
                onPronounce = viewModel::pronounceCount,
            )
        }

        // --- Shared quiz --------------------------------------------------------

        composable(
            route = Route.Quiz.pattern,
            arguments = listOf(
                navArgument(Route.Quiz.ARG_MODULE) { type = NavType.StringType },
                navArgument(Route.Quiz.ARG_ITEM_IDS) { type = NavType.StringType },
                navArgument(Route.Quiz.ARG_QUESTION_COUNT) { type = NavType.StringType },
                navArgument(Route.Quiz.ARG_SEED) { type = NavType.StringType },
            ),
        ) { entry ->
            val viewModel: QuizViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val module = entry.arguments?.getString(Route.Quiz.ARG_MODULE).toModuleType()

            Box(Modifier.fillMaxSize()) {
                QuizScreen(
                    state = state,
                    onAnswer = viewModel::onAnswerSelected,
                    onNext = viewModel::onNext,
                    onReplayPrompt = viewModel::onReplayPrompt,
                    onBack = { navController.popBackStack() },
                )

                // The result is an overlay rather than a separate destination:
                // the quiz screen's state is still the source of truth, and a
                // separate route would need the whole result re-encoded in the
                // back stack.
                state.result?.let { result ->
                    com.freedu.kidslearn.ui.quiz.QuizResultScreen(
                        result = result,
                        showCelebration = state.showCelebration,
                        onPlayAgain = {
                            viewModel.onClearedForReplay()
                            navController.popBackStack()
                            navController.navigate(quizRoute(module, entry.argumentItemId()))
                        },
                        onGoHome = {
                            navController.navigate(Route.Home.pattern) {
                                popUpTo(Route.Home.pattern) { inclusive = true }
                            }
                        },
                        onDismissCelebration = viewModel::dismissCelebration,
                    )
                }
            }
        }

        // --- Games ---------------------------------------------------------------

        composable(Route.GamesHub.pattern) {
            val viewModel: GamesHubViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GamesHubScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onOpenGame = { gameType ->
                    navController.navigate(
                        when (gameType) {
                            com.freedu.kidslearn.domain.model.GameType.MEMORY_MATCH -> Route.MemoryMatch.pattern
                            com.freedu.kidslearn.domain.model.GameType.FIND_THE_CORRECT_ONE -> Route.TapTheAnswer.pattern
                            com.freedu.kidslearn.domain.model.GameType.TIMED_QUIZ -> Route.TimedQuiz.pattern
                        },
                    )
                },
            )
        }

        composable(Route.MemoryMatch.pattern) {
            val viewModel: MemoryMatchViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize()) {
                MemoryMatchScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onCardTapped = viewModel::onCardTapped,
                )
                state.result?.let { result ->
                    com.freedu.kidslearn.ui.quiz.GameResultScreen(
                        result = result,
                        showCelebration = state.showCelebration,
                        onPlayAgain = viewModel::startNewGame,
                        onGoHome = {
                            navController.navigate(Route.Home.pattern) {
                                popUpTo(Route.Home.pattern) { inclusive = true }
                            }
                        },
                        onDismissCelebration = viewModel::dismissResult,
                    )
                }
            }
        }

        composable(Route.TapTheAnswer.pattern) {
            val viewModel: FindCorrectViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize()) {
                FindCorrectScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onOptionTapped = viewModel::onOptionTapped,
                )
                state.result?.let { result ->
                    com.freedu.kidslearn.ui.quiz.GameResultScreen(
                        result = result,
                        showCelebration = state.showCelebration,
                        onPlayAgain = viewModel::playAgain,
                        onGoHome = {
                            navController.navigate(Route.Home.pattern) {
                                popUpTo(Route.Home.pattern) { inclusive = true }
                            }
                        },
                        onDismissCelebration = viewModel::dismissResult,
                    )
                }
            }
        }

        composable(Route.TimedQuiz.pattern) {
            val viewModel: TimedQuizViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize()) {
                TimedQuizScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onAnswerSelected = viewModel::onAnswerSelected,
                )
                state.result?.let { result ->
                    com.freedu.kidslearn.ui.quiz.GameResultScreen(
                        result = result,
                        showCelebration = state.showCelebration,
                        onPlayAgain = viewModel::startRound,
                        onGoHome = {
                            navController.navigate(Route.Home.pattern) {
                                popUpTo(Route.Home.pattern) { inclusive = true }
                            }
                        },
                        onDismissCelebration = viewModel::dismissResult,
                    )
                }
            }
        }

        // --- Parents -------------------------------------------------------------

        composable(Route.Progress.pattern) {
            val viewModel: ProgressViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ProgressScreen(
                state = state,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.ParentZone.pattern) {
            val viewModel: ParentZoneViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ParentZoneScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRequireGate = viewModel::requireGate,
                onDismissGate = viewModel::dismissGate,
                onAnswerChanged = viewModel::onAnswerChanged,
                onSubmitAnswer = viewModel::submitGateAnswer,
                onSetSound = viewModel::setSoundEnabled,
                onSetMusic = viewModel::setMusicEnabled,
                onSetLanguage = viewModel::setLanguage,
                onSetTheme = viewModel::setThemeMode,
                onSetChildName = viewModel::setChildName,
                onSetParentGate = viewModel::setParentGateEnabled,
                onResetProgress = viewModel::onResetProgress,
                onAcknowledgeReset = viewModel::acknowledgeReset,
            )
        }
    }
}

// --------------------------------------------------------------------- helpers

private fun routeForModule(module: ModuleType): String = when (module) {
    ModuleType.ENGLISH -> Route.AlphabetModule.createRoute(ModuleType.ENGLISH)
    ModuleType.BANGLA -> Route.AlphabetModule.createRoute(ModuleType.BANGLA)
    ModuleType.ARABIC -> Route.AlphabetModule.createRoute(ModuleType.ARABIC)
    ModuleType.MATHS -> Route.MathsModule.pattern
}

private fun quizRoute(module: ModuleType, itemId: String?): String =
    Route.Quiz.createRoute(
        module = module,
        itemIds = listOfNotNull(itemId),
        questionCount = if (itemId == null) 5 else 3,
    )

private fun String?.toModuleType(): ModuleType =
    ModuleType.entries.firstOrNull { it.name == this } ?: ModuleType.ENGLISH

private fun androidx.navigation.NavBackStackEntry.argumentModule(): ModuleType =
    arguments?.getString(Route.AlphabetModule.ARG_MODULE).toModuleType()

private fun androidx.navigation.NavBackStackEntry.argumentItemId(): String? =
    arguments?.getString(Route.Quiz.ARG_ITEM_IDS)
        ?.takeIf { it.isNotBlank() && it != Route.Quiz.NO_ITEMS }
        ?.substringBefore(',')
        ?.let { decode(it) }
