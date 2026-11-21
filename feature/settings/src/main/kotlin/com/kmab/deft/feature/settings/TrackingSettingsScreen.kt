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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftSettingsTopBar
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftSwitch
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.TrackingPreferences

@Composable
fun TrackingSettingsScreen(
    onBack: () -> Unit,
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
    TrackingSettingsContent(preferences, onBack, viewModel::update)
}

@Composable
internal fun TrackingSettingsContent(
    preferences: TrackingPreferences,
    onBack: () -> Unit,
    update: ((TrackingPreferences) -> TrackingPreferences) -> Unit,
) {
    val c = LocalDeftColors.current
    Scaffold(containerColor = c.bg, contentColor = c.text, topBar = {
        DeftSettingsTopBar("tracking", navigationIcon = {
            IconButton(onBack) { Icon(DeftActionIcons.Back, "back") }
        })
    }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                MinimumSessionSetting(preferences.minimumSessionSeconds) { value ->
                    update { it.copy(minimumSessionSeconds = value) }
                }
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

@Composable
private fun MinimumSessionSetting(
    seconds: Int,
    save: (Int) -> Unit,
) {
    var text by rememberSaveable(seconds) { mutableStateOf(seconds.toString()) }
    val unit = if (seconds == 1) "second" else "seconds"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("minimum session · $seconds $unit", style = DeftTypography.label)
        val range = TrackingPreferences.MINIMUM_SESSION_RANGE
        DurationField("minimum session", "seconds", text, range, { text = it }, save)
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
        DeftSwitch(selected, change, modifier = Modifier.semanticsLabel(title))
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
            val range = 1..TrackingPreferences.MAX_IDLE_MINUTES
            DurationField(label, "minutes", text, range, { text = it }, save)
        }
    }
}

@Composable
private fun DurationField(
    setting: String,
    unit: String,
    text: String,
    range: IntRange,
    change: (String) -> Unit,
    save: (Int) -> Unit,
) {
    val colors = LocalDeftColors.current
    var invalid by rememberSaveable(setting) { mutableStateOf(false) }
    val apply = {
        val value = text.toIntOrNull()?.takeIf { it in range }
        invalid = value == null
        if (value != null) save(value)
    }
    val help =
        if (invalid) {
            "Enter a whole number from ${range.first} to ${range.last}."
        } else {
            "${range.first}–${range.last} $unit"
        }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    invalid = false
                    change(it)
                },
                modifier =
                    Modifier.weight(1f).semanticsLabel("$setting · $unit").semantics {
                        if (invalid) error(help)
                    },
                label = { Text(unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { apply() }),
                singleLine = true,
                isError = invalid,
            )
            DeftButton(
                "apply",
                apply,
                modifier = Modifier.semanticsLabel("apply $setting"),
                style = DeftButtonStyle.Text,
                icon = DeftActionIcons.Check,
            )
        }
        Text(help, style = DeftTypography.mono, color = if (invalid) colors.error else colors.muted)
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(
        Modifier.semantics {
            contentDescription = label
        },
    )
