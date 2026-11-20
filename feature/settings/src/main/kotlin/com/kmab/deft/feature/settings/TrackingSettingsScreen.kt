package com.kmab.deft.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.TrackingPreferences

@Composable
fun TrackingSettingsScreen(
    onBack: () -> Unit,
    appearance: Boolean = false,
    viewModel: TrackingSettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsState()
    val error by viewModel.error.collectAsState()
    val notices = LocalDeftNoticeState.current
    LaunchedEffect(error) {
        error?.let {
            notices?.show(it, DeftNoticeKind.Error)
            viewModel.error.value = null
        }
    }
    TrackingSettingsContent(preferences, onBack, viewModel::update, appearance)
}

@Composable
internal fun TrackingSettingsContent(
    preferences: TrackingPreferences,
    onBack: () -> Unit,
    update: ((TrackingPreferences) -> TrackingPreferences) -> Unit,
    appearance: Boolean = false,
) {
    val c = LocalDeftColors.current
    Scaffold(containerColor = c.bg, contentColor = c.text, topBar = {
        DeftTopBar(if (appearance) "appearance" else "tracking", navigationIcon = {
            IconButton(onBack) { Icon(DeftActionIcons.Back, "back") }
        })
    }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (appearance) {
                PreferenceCard {
                    SettingSwitch(
                        "celebrations",
                        "A short finish animation, followed by your rating. " +
                            "When off, only the rating sheet appears.",
                        preferences.celebrations,
                    ) { value -> update { it.copy(celebrations = value) } }
                    Text(
                        "Follows Android's animation preference. App colors follow your system theme.",
                        style = DeftTypography.body,
                        color = c.muted,
                    )
                }
            } else {
                PreferenceCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(DeftActionIcons.History, null, tint = c.muted)
                        Text("idle pause", style = DeftTypography.title)
                    }
                    Text(
                        "Pause after no page turns, scrolling or selection. Idle gaps are not counted.",
                        style = DeftTypography.body,
                        color = c.muted,
                    )
                    TimeoutSetting("reflowable books", preferences.reflowableIdleMinutes) { value ->
                        update {
                            it.copy(reflowableIdleMinutes = value)
                        }
                    }
                    HorizontalDivider(color = c.line)
                    TimeoutSetting("fixed-layout books", preferences.fixedLayoutIdleMinutes) { value ->
                        update {
                            it.copy(fixedLayoutIdleMinutes = value)
                        }
                    }
                    Text(
                        "With idle pause off, keep screen on is released after 15 minutes without interaction. " +
                            "Screen-off always ends the session.",
                        style = DeftTypography.body,
                        color = c.muted,
                    )
                }
                PreferenceCard {
                    SettingSwitch(
                        "reading indicator",
                        "A quiet indicator in the reader while tracking is active.",
                        preferences.showIndicator,
                    ) { value ->
                        update { it.copy(showIndicator = value) }
                    }
                    HorizontalDivider(color = c.line)
                    Text("minimum session · 60 seconds", style = DeftTypography.label)
                    Text(
                        "Shorter visits are discarded. Just browsing stays in a book's history " +
                            "and is excluded from reading totals.",
                        style = DeftTypography.body,
                        color = c.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceCard(content: @Composable ColumnScope.() -> Unit) {
    val c = LocalDeftColors.current
    Surface(shape = DeftShapes.card, color = c.surface, border = BorderStroke(1.dp, c.line), shadowElevation = 1.dp) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    selected: Boolean,
    change: (Boolean) -> Unit,
) {
    val c = LocalDeftColors.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = DeftTypography.label)
            Text(description, style = DeftTypography.body, color = c.muted)
        }
        Switch(selected, change, modifier = Modifier.semanticsLabel(title))
    }
}

@Composable
private fun TimeoutSetting(
    label: String,
    minutes: Int?,
    save: (Int?) -> Unit,
) {
    val defaultMinutes =
        if (label ==
            "fixed-layout books"
        ) {
            TrackingPreferences.DEFAULT_FIXED_MINUTES
        } else {
            TrackingPreferences.DEFAULT_REFLOWABLE_MINUTES
        }
    var text by rememberSaveable(label, minutes) { mutableStateOf((minutes ?: defaultMinutes).toString()) }
    var invalid by rememberSaveable(label) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingSwitch(
            label,
            if (minutes ==
                null
            ) {
                "off"
            } else {
                if (minutes == 1) "1 minute" else "$minutes minutes"
            },
            minutes != null,
        ) {
            save(
                if (it) {
                    text.toIntOrNull()?.coerceIn(1, TrackingPreferences.MAX_IDLE_MINUTES)
                        ?: defaultMinutes
                } else {
                    null
                },
            )
        }
        if (minutes != null) {
            OutlinedTextField(
                text,
                {
                    text = it
                    invalid = false
                },
                Modifier.fillMaxWidth().semantics { contentDescription = "$label · minutes" },
                label = {
                    Text("minutes")
                },
                supportingText = { Text(if (invalid) "Enter a whole number from 1 to 60." else "1–60 minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = invalid,
            )
            DeftButton(
                "apply",
                {
                    val value = text.toIntOrNull()
                    if (value == null ||
                        value !in 1..TrackingPreferences.MAX_IDLE_MINUTES
                    ) {
                        invalid = true
                    } else {
                        save(value)
                    }
                },
                modifier =
                    Modifier.semantics {
                        contentDescription = "apply $label"
                    },
                style = DeftButtonStyle.Text,
                icon = DeftActionIcons.Check,
            )
        }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(
        Modifier.semantics {
            contentDescription = label
        },
    )
