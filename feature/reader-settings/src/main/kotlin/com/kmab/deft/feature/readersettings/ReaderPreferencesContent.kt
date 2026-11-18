package com.kmab.deft.feature.readersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
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
    val colors = LocalDeftColors.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth().then(if (manageAssets) Modifier else Modifier.heightIn(max = 560.dp))) {
        TabRow(selectedTabIndex = tab, containerColor = colors.surface, contentColor = colors.accentText) {
            listOf("text", "theme", "page").forEachIndexed { index, label ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    selectedContentColor = colors.accentText,
                    unselectedContentColor = colors.muted,
                    text = { Text(label, style = DeftTypography.label) },
                )
            }
        }
        AnimatedContent(tab, label = "reader settings tab", modifier = Modifier.weight(1f, fill = false)) { selected ->
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
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
    val colors = LocalDeftColors.current
    ReaderSettingGroup("typeface & size") {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = choose),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Aa", style = DeftTypography.headline, color = colors.muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("font", style = DeftTypography.body, color = colors.muted)
                Text(fonts.firstOrNull { it.id == p.fontFamily }?.name ?: "Literata", style = DeftTypography.title)
            }
            Icon(DeftActionIcons.ChevronRight, null, Modifier.size(20.dp), tint = colors.muted)
        }
        SettingDivider()
        Stepper(
            "text size",
            "${(p.fontSize * PERCENT).toInt()}%",
            p.fontSize > ReaderPreferences.MIN_FONT_SIZE,
            p.fontSize < ReaderPreferences.MAX_FONT_SIZE,
            { update { it.copy(fontSize = it.fontSize - STEP) } },
            { update { it.copy(fontSize = it.fontSize + STEP) } },
        )
        SettingDivider()
        Stepper(
            "line height",
            String.format(Locale.ROOT, "%.1f", p.lineHeight),
            p.lineHeight > ReaderPreferences.MIN_LINE_HEIGHT,
            p.lineHeight < ReaderPreferences.MAX_LINE_HEIGHT,
            { update { it.copy(lineHeight = it.lineHeight - STEP) } },
            { update { it.copy(lineHeight = it.lineHeight + STEP) } },
        )
    }
    ReaderSettingGroup("paragraph") {
        Choices("margins", ReaderMargins.entries, p.margins, { it.name.lowercase(Locale.ROOT) }) { value ->
            update { it.copy(margins = value) }
        }
        SettingDivider()
        Choices("alignment", ReaderAlignment.entries, p.alignment, {
            if (it == ReaderAlignment.START) "left" else "justify"
        }) { value -> update { it.copy(alignment = value) } }
        SettingDivider()
        PreferenceToggle("hyphenation", p.hyphenation) { value -> update { it.copy(hyphenation = value) } }
    }
}

@Composable
private fun PagePreferences(
    p: ReaderPreferences,
    update: PreferenceUpdate,
) {
    ReaderSettingGroup("navigation") {
        Choices("reading mode", listOf(false, true), p.scroll, { if (it) "scroll" else "paged" }) { value ->
            update { it.copy(scroll = value) }
        }
        SettingDivider()
        Choices("page animation", PageAnimation.entries, p.pageAnimation, {
            it.name.lowercase(Locale.ROOT)
        }, enabled = !p.scroll) { value -> update { it.copy(pageAnimation = value) } }
    }
    ReaderSettingGroup("while reading") {
        PreferenceToggle("show status line", p.showStatusLine) { value -> update { it.copy(showStatusLine = value) } }
        SettingDivider()
        PreferenceToggle("volume keys turn pages", p.volumeKeys) { value -> update { it.copy(volumeKeys = value) } }
        SettingDivider()
        PreferenceToggle("keep screen on", p.keepScreenOn) { value -> update { it.copy(keepScreenOn = value) } }
    }
}

private const val STEP = 0.1
internal const val PERCENT = 100
