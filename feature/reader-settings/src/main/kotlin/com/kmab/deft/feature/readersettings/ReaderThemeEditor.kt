package com.kmab.deft.feature.readersettings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.rememberDeftNoticeState
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.core.model.ReaderThemes
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@Composable
internal fun ReaderThemeEditor(
    initial: ReaderTheme?,
    onSave: suspend (ReaderTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val id = remember(initial) { initial?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(id) { mutableStateOf(initial?.name.orEmpty()) }
    var background by rememberSaveable(id) { mutableStateOf(initial?.background ?: "#FBF9F4") }
    var text by rememberSaveable(id) { mutableStateOf(initial?.text ?: "#1B1B1F") }
    var accent by rememberSaveable(id) { mutableStateOf(initial?.accent ?: "#7D34B2") }
    var saving by remember { mutableStateOf(false) }
    val notices = rememberDeftNoticeState()
    val scope = rememberCoroutineScope()
    val theme = ReaderTheme(id, name.trim(), background, text, accent)
    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Surface {
            Box(Modifier.fillMaxWidth().imePadding()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (initial ==
                                null
                            ) {
                                "new theme"
                            } else {
                                "edit theme"
                            },
                            Modifier.weight(1f),
                            style = DeftTypography.title,
                        )
                        IconButton(
                            onClick = onDismiss,
                            enabled = !saving,
                        ) { Icon(DeftActionIcons.Close, "close theme editor") }
                    }
                    ThemeField("name", name) { name = it.take(MAX_NAME) }
                    ThemeField("background", background) { background = it.take(HEX_LENGTH) }
                    ThemeField("text", text) { text = it.take(HEX_LENGTH) }
                    ThemeField("links and accents", accent) { accent = it.take(HEX_LENGTH) }
                    ThemePreview(theme)
                    DeftButton("save theme", enabled = !saving, onClick = {
                        val error = ReaderThemes.validationError(theme)
                        if (error != null) {
                            notices.show(error, DeftNoticeKind.Error)
                        } else {
                            saving = true
                            scope.launch {
                                try {
                                    onSave(theme)
                                    onDismiss()
                                } catch (_: IOException) {
                                    notices.show("The theme could not be saved. Try again.", DeftNoticeKind.Error)
                                } catch (invalid: IllegalArgumentException) {
                                    notices.show(invalid.message ?: "Check the theme colors.", DeftNoticeKind.Error)
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth())
                }
                DeftNoticeHost(notices, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun ThemeField(
    label: String,
    value: String,
    update: (String) -> Unit,
) {
    OutlinedTextField(value, update, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ThemePreview(theme: ReaderTheme) {
    val background by animateColorAsState(safeColor(theme.background, "#FBF9F4"), label = "preview background")
    val text by animateColorAsState(safeColor(theme.text, "#1B1B1F"), label = "preview text")
    val accent by animateColorAsState(safeColor(theme.accent, "#7D34B2"), label = "preview accent")
    Column(
        Modifier.fillMaxWidth().background(background).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("A quiet place to read.", color = text, style = DeftTypography.body)
        Text("chapter one", color = accent, style = DeftTypography.label)
    }
}

private fun safeColor(
    value: String,
    fallback: String,
) = themeColor(value.takeIf { Regex("#[a-fA-F0-9]{6}").matches(it) } ?: fallback)

private const val MAX_NAME = 40
private const val HEX_LENGTH = 7
