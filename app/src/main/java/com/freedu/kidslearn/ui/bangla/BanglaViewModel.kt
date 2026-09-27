package com.freedu.kidslearn.ui.bangla

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
 * The Bangla (বাংলা) module: 11 স্বরবর্ণ and 35 ব্যঞ্জনবর্ণ.
 *
 * A pure binding, exactly as in the English module - see [EnglishViewModel] for
 * why the behaviour lives in the shared [AlphabetViewModel].
 */
@HiltViewModel
class BanglaViewModel @Inject constructor(
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
    moduleType = ModuleType.BANGLA,
)
