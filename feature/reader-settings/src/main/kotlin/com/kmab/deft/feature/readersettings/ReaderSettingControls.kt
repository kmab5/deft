package com.kmab.deft.feature.readersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors

@Composable
internal fun ReaderSettingGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = DeftTypography.label, color = colors.muted)
        Surface(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = DeftShapes.card,
            color = colors.surface,
            contentColor = colors.text,
            border = BorderStroke(1.dp, colors.line),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), content = content)
        }
    }
}

@Composable
internal fun SettingDivider() = HorizontalDivider(color = LocalDeftColors.current.line)

@Composable
internal fun Stepper(
    label: String,
    value: String,
    decrease: Boolean,
    increase: Boolean,
    down: () -> Unit,
    up: () -> Unit,
) {
    val colors = LocalDeftColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f), style = DeftTypography.body)
        Surface(shape = DeftShapes.small, color = colors.surface2, border = BorderStroke(1.dp, colors.line)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = down,
                    enabled = decrease,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "decrease $label"
                        },
                ) {
                    Text("−", style = DeftTypography.headline, color = if (decrease) colors.text else colors.dim)
                }
                AnimatedContent(value, label = "$label value") { number ->
                    Text(
                        number,
                        Modifier.widthIn(min = 48.dp),
                        style = DeftTypography.label,
                        color = colors.accentText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                IconButton(
                    onClick = up,
                    enabled = increase,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "increase $label"
                        },
                ) {
                    Text("+", style = DeftTypography.headline, color = if (increase) colors.text else colors.dim)
                }
            }
        }
    }
}

@Composable
internal fun PreferenceToggle(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = LocalDeftColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f), style = DeftTypography.body)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.muted,
                    uncheckedTrackColor = colors.surface2,
                    uncheckedBorderColor = colors.line2,
                ),
        )
    }
}

@Composable
internal fun <T> Choices(
    label: String,
    values: List<T>,
    selected: T,
    name: (T) -> String,
    enabled: Boolean = true,
    change: (T) -> Unit,
) {
    val colors = LocalDeftColors.current
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = DeftTypography.body, color = if (enabled) colors.text else colors.muted)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEach { value ->
                val active = selected == value
                val background by animateColorAsState(
                    if (active) colors.surface2 else colors.surface,
                    label = "$label selection",
                )
                val border by animateColorAsState(
                    if (active && enabled) colors.accentText else colors.line,
                    label = "$label outline",
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = DeftShapes.small,
                    color = background,
                    border = BorderStroke(1.dp, border),
                ) {
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .selectable(active, enabled = enabled, role = Role.RadioButton, onClick = { change(value) })
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name(value),
                            style = DeftTypography.label,
                            color = if (active && enabled) colors.accentText else colors.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
