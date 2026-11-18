package com.kmab.deft.feature.readersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.core.model.ReaderThemes

@Composable
internal fun ThemePreferences(
    p: ReaderPreferences,
    custom: List<ReaderTheme>,
    update: PreferenceUpdate,
    edit: (ReaderTheme?) -> Unit,
    delete: (ReaderTheme) -> Unit,
    manage: Boolean,
) {
    val themes = ReaderThemes.builtIn + custom
    ReaderSettingGroup("appearance") {
        PreferenceToggle("follow system theme", p.followSystem) { value -> update { it.copy(followSystem = value) } }
        SettingDivider()
        AnimatedContent(p.followSystem, label = "theme policy") { automatic ->
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (automatic) {
                    ThemeChoices("when system is light", themes, p.lightThemeId) { id ->
                        update { it.copy(lightThemeId = id) }
                    }
                    ThemeChoices("when system is dark", themes, p.darkThemeId) { id ->
                        update { it.copy(darkThemeId = id) }
                    }
                } else {
                    ThemeChoices("reading theme", themes, p.themeId) { id -> update { it.copy(themeId = id) } }
                }
            }
        }
        SettingDivider()
        TextButton(onClick = { edit(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("+ new theme", color = LocalDeftColors.current.accentText)
        }
    }
    if (manage && custom.isNotEmpty()) {
        ReaderSettingGroup("your themes") {
            custom.forEachIndexed { index, theme ->
                if (index > 0) SettingDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).heightIn(min = 56.dp).clickable(role = Role.Button) { edit(theme) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(color = themeColor(theme.background), shape = DeftShapes.small) {
                            Text("Aa", Modifier.padding(8.dp), color = themeColor(theme.text))
                        }
                        Text("edit ${theme.name}", style = DeftTypography.body)
                    }
                    IconButton(onClick = { delete(theme) }) {
                        Icon(DeftActionIcons.Trash, "delete ${theme.name}", tint = LocalDeftColors.current.muted)
                    }
                }
            }
        }
    }
    ReaderSettingGroup("brightness") {
        PreferenceToggle("use system brightness", p.useSystemBrightness) { value ->
            update { it.copy(useSystemBrightness = value) }
        }
        SettingDivider()
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("reader brightness", Modifier.weight(1f), style = DeftTypography.body)
            Text(
                if (p.useSystemBrightness) "system" else "${(p.brightness * PERCENT).toInt()}%",
                style = DeftTypography.label,
                color = LocalDeftColors.current.muted,
            )
        }
        Slider(
            value = p.brightness,
            onValueChange = { value -> update { it.copy(brightness = value) } },
            enabled = !p.useSystemBrightness,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "reader brightness" },
        )
    }
}

@Composable
private fun ThemeChoices(
    label: String,
    themes: List<ReaderTheme>,
    selected: String,
    choose: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = DeftTypography.body)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themes.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { theme ->
                        ThemeSwatch(theme, theme.id == selected, { choose(theme.id) }, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: ReaderTheme,
    selected: Boolean,
    choose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDeftColors.current
    val border by animateColorAsState(if (selected) colors.accentText else colors.line2, label = "selected theme")
    Surface(
        modifier = modifier,
        color = themeColor(theme.background),
        contentColor = themeColor(theme.text),
        shape = DeftShapes.small,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
    ) {
        Column(
            Modifier
                .heightIn(min = 80.dp)
                .selectable(selected, role = Role.RadioButton, onClick = choose)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Aa", style = DeftTypography.headline, fontFamily = FontFamily.Serif)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    theme.name,
                    Modifier.weight(1f),
                    style = DeftTypography.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) Icon(DeftActionIcons.Check, null, Modifier.size(16.dp))
            }
        }
    }
}

internal fun themeColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))
