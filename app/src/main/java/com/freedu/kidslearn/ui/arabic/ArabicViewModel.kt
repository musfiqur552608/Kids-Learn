package com.freedu.kidslearn.ui.arabic

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
 * The Arabic (حروف) module: the 28 letters in isolated form.
 *
 * The module's RTL requirement is a *presentation* concern, so it lives in
 * [ArabicModuleScreen] which wraps the shared screen in `ProvideRtl`. Nothing in
 * the ViewModel, the domain or the content catalog has to know about layout
 * direction - the glyphs and the lesson ids are script-agnostic strings.
 */
@HiltViewModel
class ArabicViewModel @Inject constructor(
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
    moduleType = ModuleType.ARABIC,
)
