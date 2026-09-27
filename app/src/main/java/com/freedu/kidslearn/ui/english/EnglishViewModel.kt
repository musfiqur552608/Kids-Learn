package com.freedu.kidslearn.ui.english

import androidx.lifecycle.SavedStateHandle
import com.freedu.kidslearn.core.audio.FeedbackPlayer
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.repository.ContentRepository
import com.freedu.kidslearn.domain.repository.ProgressRepository
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.domain.usecase.RememberLastModuleUseCase
import com.freedu.kidslearn.ui.alphabet.AlphabetViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * The English A-Z module.
 *
 * A pure binding: it exists only to pin [ModuleType.ENGLISH], which Hilt cannot
 * inject on its own. All behaviour lives in [AlphabetViewModel], shared with the
 * Bangla and Arabic modules so a fix to the quiz entry point or the progress bar
 * cannot be made for one subject and forgotten for the others.
 */
@HiltViewModel
class EnglishViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    rememberLastModule: RememberLastModuleUseCase,
    feedbackPlayer: FeedbackPlayer,
) : AlphabetViewModel(
    savedStateHandle = savedStateHandle,
    contentRepository = contentRepository,
    progressRepository = progressRepository,
    settingsRepository = settingsRepository,
    rememberLastModule = rememberLastModule,
    feedbackPlayer = feedbackPlayer,
    moduleType = ModuleType.ENGLISH,
)
