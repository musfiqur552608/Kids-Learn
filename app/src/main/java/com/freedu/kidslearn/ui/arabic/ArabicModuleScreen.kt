package com.freedu.kidslearn.ui.arabic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.alphabet.AlphabetModuleScreen
import com.freedu.kidslearn.ui.theme.ProvideRtl

/**
 * The Arabic module screen.
 *
 * ## Where the RTL switch lives, and why
 * [ProvideRtl] wraps the *whole* Arabic subtree, so the top bar, the letter grid
 * and the progress bar all mirror. Doing it per-widget would be an easy thing to
 * forget, and a half-mirrored layout is worse for a child than either direction.
 *
 * The direction change is scoped to this screen only. The app chrome (Home, the
 * parent zone) stays left-to-right regardless of the device locale, because those
 * screens are authored LTR and mixing the two would misplace the back button
 * relative to the child's other mental model.
 */
@Composable
fun ArabicModuleScreen(
    onBack: () -> Unit,
    onTakeQuiz: (module: ModuleType, itemId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArabicViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProvideRtl {
        AlphabetModuleScreen(
            state = state,
            onBack = onBack,
            onShowOverview = viewModel::showOverview,
            onShowLetterList = viewModel::showLetterList,
            onSelectLetter = viewModel::selectLetter,
            onClearSelection = viewModel::clearSelection,
            onPronounce = viewModel::pronounce,
            onTakeQuiz = { item -> onTakeQuiz(ModuleType.ARABIC, item?.id) },
            modifier = modifier,
        )
    }
}
