package com.freedu.kidslearn.ui.bangla

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.alphabet.AlphabetModuleScreen

/**
 * The Bangla module screen. See [EnglishModuleScreen] for the rationale behind
 * these thin bindings.
 */
@Composable
fun BanglaModuleScreen(
    onBack: () -> Unit,
    onTakeQuiz: (module: ModuleType, itemId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BanglaViewModel = hiltViewModel(),
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
        onTakeQuiz = { item -> onTakeQuiz(ModuleType.BANGLA, item?.id) },
        modifier = modifier,
    )
}
