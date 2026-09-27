package com.freedu.kidslearn.ui.parentzone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.model.UiLanguage
import com.freedu.kidslearn.ui.alphabet.labelRes
import com.freedu.kidslearn.ui.components.BigStat
import com.freedu.kidslearn.ui.components.KidButton
import com.freedu.kidslearn.ui.components.KidIconButton
import com.freedu.kidslearn.ui.components.LabelledProgress
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import com.freedu.kidslearn.ui.theme.MinTouchTarget
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * The parent zone: statistics and settings, behind a maths gate.
 *
 * ## Density and tone
 * This is the only screen written *for the parent*, so it is intentionally denser
 * and quieter than the rest of the app: small text, no mascot speech, no
 * animation, and no celebratory colour. The child-facing screens are bright and
 * loud; this one is not, which is itself a signal that it belongs to an adult.
 */
@Composable
fun ParentZoneScreen(
    state: ParentZoneUiState,
    onBack: () -> Unit,
    onRequireGate: () -> Unit,
    onDismissGate: () -> Unit,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetMusic: (Boolean) -> Unit,
    onSetLanguage: (UiLanguage) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetChildName: (String) -> Unit,
    onSetParentGate: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onAcknowledgeReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = KidTheme.colors.parentZone

    // Ask for the gate as soon as the parent arrives, unless they have switched it
    // off deliberately.
    LaunchedEffect(state.settings.parentGateEnabled) {
        if (state.settings.parentGateEnabled) onRequireGate()
    }

    if (state.resetDone) {
        ResetDoneDialog(onDismiss = onAcknowledgeReset)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KidIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                containerColor = Color.White.copy(alpha = 0.22f),
                contentColor = Color.White,
            )
            Text(
                text = stringResource(R.string.parent_zone_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatsSection(state) }
            item { SettingsSection(state, onSetSound, onSetMusic, onSetLanguage, onSetTheme, onSetChildName) }
            item { GateToggleSection(state, onSetParentGate) }
            item { ResetSection(onResetProgress) }
            item { AboutSection() }
        }
    }

    // The gate is a dialog rather than a separate screen: it must be impossible to
    // scroll past it, and a child who taps outside must not land in the settings.
    when (val gate = state.gate) {
        is ParentGateState.Awaiting -> ParentGateDialog(
            state = gate,
            onAnswerChanged = onAnswerChanged,
            onSubmit = onSubmitAnswer,
            onDismiss = onDismissGate,
        )

        ParentGateState.Passed, ParentGateState.Hidden -> Unit
    }
}

@Composable
private fun StatsSection(state: ParentZoneUiState) {
    SectionCard {
        Text(
            text = stringResource(R.string.progress_title),
            style = MaterialTheme.typography.titleMedium,
            color = KidTheme.colors.parentZone,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigStat(
                value = state.dashboard.stats.totalStars.toString(),
                label = stringResource(R.string.progress_total_stars),
                color = KidTheme.colors.starGold,
                modifier = Modifier.weight(1f),
            )
            BigStat(
                value = state.dashboard.stats.totalCoins.toString(),
                label = stringResource(R.string.progress_total_coins),
                color = KidTheme.colors.coinGold,
                modifier = Modifier.weight(1f),
            )
            BigStat(
                value = state.dashboard.stats.lessonsCompleted.toString(),
                label = stringResource(R.string.progress_lessons),
                color = KidTheme.colors.progress,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        state.dashboard.modules.forEach { module ->
            LabelledProgress(
                title = androidx.compose.ui.res.stringResource(module.moduleType.labelRes()),
                completed = module.completedItems,
                total = module.totalItems,
                barColor = KidTheme.colors.accentFor(module.moduleType),
                percentText = pluralStringResource(R.plurals.progress_percent, module.completionPercent, module.completionPercent),
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    state: ParentZoneUiState,
    onSetSound: (Boolean) -> Unit,
    onSetMusic: (Boolean) -> Unit,
    onSetLanguage: (UiLanguage) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetChildName: (String) -> Unit,
) {
    val settings = state.settings

    SectionCard {
        Text(
            text = stringResource(R.string.module_parent_zone),
            style = MaterialTheme.typography.titleMedium,
            color = KidTheme.colors.parentZone,
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = settings.childName,
            onValueChange = onSetChildName,
            label = { Text(stringResource(R.string.parent_zone_child_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        SettingSwitch(
            label = stringResource(R.string.parent_zone_sound),
            checked = settings.soundEnabled,
            onCheckedChange = onSetSound,
        )
        SettingSwitch(
            label = stringResource(R.string.parent_zone_music),
            checked = settings.musicEnabled,
            onCheckedChange = onSetMusic,
        )

        Spacer(Modifier.height(12.dp))
        ChoiceRow(
            label = stringResource(R.string.parent_zone_language),
            options = UiLanguage.entries.map { it to androidx.compose.ui.res.stringResource(it.labelRes()) },
            selected = settings.uiLanguage,
            onSelect = onSetLanguage,
        )
        ChoiceRow(
            label = stringResource(R.string.parent_zone_theme),
            options = ThemeMode.entries.map { it to androidx.compose.ui.res.stringResource(it.labelRes()) },
            selected = settings.themeMode,
            onSelect = onSetTheme,
        )
    }
}

@Composable
private fun GateToggleSection(
    state: ParentZoneUiState,
    onSetParentGate: (Boolean) -> Unit,
) {
    SectionCard {
        SettingSwitch(
            label = stringResource(R.string.parent_zone_gate_toggle),
            checked = state.settings.parentGateEnabled,
            onCheckedChange = onSetParentGate,
        )
    }
}

@Composable
private fun ResetSection(onResetProgress: () -> Unit) {
    SectionCard {
        Text(
            text = stringResource(R.string.parent_zone_reset),
            style = MaterialTheme.typography.titleMedium,
            // The only red in the app. It is the one destructive action.
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.parent_zone_reset_confirm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onResetProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.parent_zone_reset_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun AboutSection() {
    SectionCard {
        Text(
            text = stringResource(R.string.parent_zone_about),
            style = MaterialTheme.typography.titleMedium,
            color = KidTheme.colors.parentZone,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.parent_zone_offline_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.parent_zone_version,
                com.freedu.kidslearn.BuildConfig.VERSION_NAME,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -------------------------------------------------------------------- pieces

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                val isSelected = value == selected
                Button(
                    onClick = { onSelect(value) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ParentGateDialog(
    state: ParentGateState.Awaiting,
    onAnswerChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.parent_gate_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(mood = MascotMood.HAPPY, size = 72.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = pluralStringResource(R.plurals.parent_gate_question, 1, state.question.prompt),
                    style = MaterialTheme.typography.displaySmall,
                    color = KidTheme.colors.parentZone,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.entered,
                    onValueChange = onAnswerChanged,
                    singleLine = true,
                    isError = state.wasWrong,
                    supportingText = if (state.wasWrong) {
                        { Text(stringResource(R.string.parent_gate_wrong)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = state.entered.isNotBlank()) {
                Text(stringResource(R.string.parent_gate_enter))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.parent_gate_cancel))
            }
        },
    )
}

@Composable
private fun ResetDoneDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.parent_zone_title)) },
        text = { Text(stringResource(R.string.parent_zone_reset_done)) },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
    )
}

/** Localised names for the two interface languages. */
private fun UiLanguage.labelRes(): Int = when (this) {
    UiLanguage.ENGLISH -> R.string.language_english
    UiLanguage.BANGLA -> R.string.language_bangla
}

/** Localised names for the two themes. */
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.parent_zone_theme_light
    ThemeMode.DARK -> R.string.parent_zone_theme_dark
}
