package com.freedu.kidslearn.ui.english

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.ui.alphabet.AlphabetModuleScreen
import com.freedu.kidslearn.ui.alphabet.AlphabetUiState

/**
 * The English module screen.
 *
 * A thin wrapper over the shared [AlphabetModuleScreen]: it binds this module's
 * ViewModel and forwards user intent. The screen itself is module-agnostic and
 * reads the module from state, so the English, Bangla and Arabic entry points
 * differ only in which ViewModel they ask Hilt for.
 */
@Composable
fun EnglishModuleScreen(
    onBack: () -> Unit,
    onTakeQuiz: (module: com.freedu.kidslearn.domain.model.ModuleType, itemId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnglishViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlphabetModuleScreen(
        state = state,
        onBack = onBack,
        onShowOverview = viewModel::showOverview,
        onShowLetterList = viewModel::showLetterList,
        onSelectLetter = viewModel::selectLetter,
        onClearSelection = viewModel::clearSelection,
        onPronounce = viewModel::pronounce,
        onTraceComplete = viewModel::onTraceFinished,
        onTakeQuiz = { item: LetterItem? ->
            onTakeQuiz(com.freedu.kidslearn.domain.model.ModuleType.ENGLISH, item?.id)
        },
        modifier = modifier,
    )
}
