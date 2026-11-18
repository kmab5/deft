package com.kmab.deft.feature.readersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.core.model.ReaderThemes
import java.util.Locale

internal typealias PreferenceUpdate = ((ReaderPreferences) -> ReaderPreferences) -> Unit

@Composable
internal fun ReaderPreferencesContent(
    preferences: ReaderPreferences,
    themes: List<ReaderTheme>,
    fonts: List<ReaderFont>,
    update: PreferenceUpdate,
    onChooseFont: () -> Unit,
    onEditTheme: (ReaderTheme?) -> Unit,
    onDeleteTheme: (ReaderTheme) -> Unit,
    modifier: Modifier = Modifier,
    manageAssets: Boolean = false,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tab) {
            listOf("text", "theme", "page").forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        AnimatedContent(tab, label = "reader settings tab", modifier = Modifier.weight(1f, fill = false)) { selected ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (selected) {
                    0 -> TextPreferences(preferences, fonts, update, onChooseFont)
                    1 -> ThemePreferences(preferences, themes, update, onEditTheme, onDeleteTheme, manageAssets)
                    else -> PagePreferences(preferences, update)
                }
            }
        }
    }
}

@Composable
private fun TextPreferences(
    p: ReaderPreferences,
    fonts: List<ReaderFont>,
    update: PreferenceUpdate,
    choose: () -> Unit,
) {
    OutlinedButton(
        onClick = choose,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = DeftShapes.small,
    ) {
        Text("font · ${fonts.firstOrNull { it.id == p.fontFamily }?.name ?: "Literata"}", Modifier.weight(1f))
        Icon(DeftActionIcons.ChevronRight, contentDescription = null)
    }
    Stepper(
        "text size",
        "${(p.fontSize * PERCENT).toInt()}%",
        p.fontSize > ReaderPreferences.MIN_FONT_SIZE,
        p.fontSize < ReaderPreferences.MAX_FONT_SIZE,
        { update { it.copy(fontSize = it.fontSize - STEP) } },
        { update { it.copy(fontSize = it.fontSize + STEP) } },
    )
    Stepper(
        "line height",
        String.format(Locale.ROOT, "%.1f", p.lineHeight),
        p.lineHeight > ReaderPreferences.MIN_LINE_HEIGHT,
        p.lineHeight < ReaderPreferences.MAX_LINE_HEIGHT,
        { update { it.copy(lineHeight = it.lineHeight - STEP) } },
        { update { it.copy(lineHeight = it.lineHeight + STEP) } },
    )
    Choices("margins", ReaderMargins.entries, p.margins, { it.name.lowercase(Locale.ROOT) }) { value ->
        update { it.copy(margins = value) }
    }
    Choices("alignment", ReaderAlignment.entries, p.alignment, {
        if (it ==
            ReaderAlignment.START
        ) {
            "left"
        } else {
            "justify"
        }
    }) { value ->
        update { it.copy(alignment = value) }
    }
    PreferenceToggle("hyphenation", p.hyphenation) { value -> update { it.copy(hyphenation = value) } }
}

@Composable
private fun ThemePreferences(
    p: ReaderPreferences,
    custom: List<ReaderTheme>,
    update: PreferenceUpdate,
    edit: (ReaderTheme?) -> Unit,
    delete: (ReaderTheme) -> Unit,
    manage: Boolean,
) {
    val themes = ReaderThemes.builtIn + custom
    Column(Modifier.animateContentSize()) {
        if (!p.followSystem) ThemeChoices("reading theme", themes, p.themeId) { id -> update { it.copy(themeId = id) } }
        PreferenceToggle("follow system theme", p.followSystem) { value -> update { it.copy(followSystem = value) } }
        if (p.followSystem) {
            ThemeChoices("when system is light", themes, p.lightThemeId) { id -> update { it.copy(lightThemeId = id) } }
            ThemeChoices("when system is dark", themes, p.darkThemeId) { id -> update { it.copy(darkThemeId = id) } }
        }
    }
    TextButton(onClick = { edit(null) }) { Text("+ new theme") }
    if (manage) {
        custom.forEach { theme ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { edit(theme) }, modifier = Modifier.weight(1f)) { Text("edit ${theme.name}") }
                IconButton(onClick = { delete(theme) }) {
                    Icon(DeftActionIcons.Trash, "delete ${theme.name}")
                }
            }
        }
    }
    PreferenceToggle("use system brightness", p.useSystemBrightness) { value ->
        update {
            it.copy(useSystemBrightness = value)
        }
    }
    Text("brightness · ${(p.brightness * PERCENT).toInt()}%", style = DeftTypography.label)
    Slider(
        value = p.brightness,
        onValueChange = { value -> update { it.copy(brightness = value) } },
        enabled = !p.useSystemBrightness,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "reader brightness" },
    )
}

@Composable
private fun PagePreferences(
    p: ReaderPreferences,
    update: PreferenceUpdate,
) {
    Choices("reading mode", listOf(false, true), p.scroll, { if (it) "scroll" else "paged" }) { value ->
        update { it.copy(scroll = value) }
    }
    Choices("page animation", PageAnimation.entries, p.pageAnimation, {
        it.name.lowercase(Locale.ROOT)
    }, enabled = !p.scroll) { value ->
        update { it.copy(pageAnimation = value) }
    }
    PreferenceToggle("show status line", p.showStatusLine) { value -> update { it.copy(showStatusLine = value) } }
    PreferenceToggle("volume keys turn pages", p.volumeKeys) { value -> update { it.copy(volumeKeys = value) } }
    PreferenceToggle("keep screen on", p.keepScreenOn) { value -> update { it.copy(keepScreenOn = value) } }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    decrease: Boolean,
    increase: Boolean,
    down: () -> Unit,
    up: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = DeftTypography.body)
            Text(value, style = DeftTypography.label, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(
            onClick = down,
            enabled = decrease,
            modifier =
                Modifier.semantics {
                    contentDescription =
                        "decrease $label"
                },
        ) {
            Text("−", style = DeftTypography.headline)
        }
        IconButton(
            onClick = up,
            enabled = increase,
            modifier =
                Modifier.semantics {
                    contentDescription =
                        "increase $label"
                },
        ) {
            Text("+", style = DeftTypography.headline)
        }
    }
}

@Composable
internal fun PreferenceToggle(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = 52.dp,
            ).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp), style = DeftTypography.body)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(
    label: String,
    values: List<T>,
    selected: T,
    name: (T) -> String,
    enabled: Boolean = true,
    change: (T) -> Unit,
) {
    Text(label, style = DeftTypography.label, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { change(value) },
                label = { Text(name(value)) },
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeChoices(
    label: String,
    themes: List<ReaderTheme>,
    selected: String,
    choose: (String) -> Unit,
) {
    Text(label, style = DeftTypography.label, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.forEach { theme ->
            FilterChip(
                selected = selected == theme.id,
                onClick = { choose(theme.id) },
                label = { Text(theme.name) },
                leadingIcon = {
                    Text(
                        "A",
                        color = themeColor(theme.text),
                        modifier = Modifier.size(24.dp).background(themeColor(theme.background)),
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

internal fun themeColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

private const val STEP = 0.1
private const val PERCENT = 100
