package com.kmab.deft.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

private const val SOURCE_URL = "https://github.com/kmab5/deft"
private const val LICENSE_URL = "$SOURCE_URL/blob/main/LICENSE"
private const val AUTHOR_URL = "https://kmab5.github.io"

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notices = LocalDeftNoticeState.current
    val versionName =
        remember(context) {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unavailable"
        }
    val icon =
        remember(context) {
            context.applicationInfo
                .loadIcon(context.packageManager)
                .toBitmap()
                .asImageBitmap()
        }
    AboutContent(
        versionName = versionName,
        onBack = onBack,
        openUri = { uri ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
            } catch (_: ActivityNotFoundException) {
                notices?.show("No browser is available to open this link.", DeftNoticeKind.Error)
            }
        },
        icon = icon,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AboutContent(
    versionName: String,
    onBack: () -> Unit,
    openUri: (String) -> Unit,
    icon: ImageBitmap? = null,
) {
    val colors = LocalDeftColors.current
    SettingsPage("about", onBack) {
        SettingsPanel {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(56.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("deft", style = DeftTypography.display)
                    Text("version $versionName", style = DeftTypography.mono, color = colors.muted)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Made by", style = DeftTypography.body)
                DeftButton(
                    "kmab",
                    { openUri(AUTHOR_URL) },
                    style = DeftButtonStyle.Text,
                    icon = DeftActionIcons.ExternalLink,
                )
            }
            HorizontalDivider(color = colors.line)
            Text("license", style = DeftTypography.title)
            Text("GNU General Public License v3.0", style = DeftTypography.body, color = colors.muted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeftButton(
                    "source code",
                    { openUri(SOURCE_URL) },
                    style = DeftButtonStyle.Secondary,
                    icon = DeftActionIcons.Source,
                )
                DeftButton(
                    "GPL-3.0",
                    { openUri(LICENSE_URL) },
                    style = DeftButtonStyle.Secondary,
                    icon = DeftActionIcons.License,
                )
            }
        }
        SettingsPanel {
            Text("privacy", style = DeftTypography.title)
            Text(
                "Deft works offline and needs no account. It stores your library and reading activity on this " +
                    "device and includes no analytics.",
                style = DeftTypography.body,
                color = colors.muted,
            )
        }
    }
}
