package com.kmab.deft.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

@Composable
internal fun NoticeContent(
    notice: DeftNotice,
    dismiss: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val actionable = notice.actionLabel != null && notice.onAction != null
    val tint =
        when (notice.kind) {
            DeftNoticeKind.Error -> colors.error
            DeftNoticeKind.Success -> colors.finishedText
            DeftNoticeKind.Info -> colors.accentText
        }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                when (notice.kind) {
                    DeftNoticeKind.Error -> DeftActionIcons.Warning
                    DeftNoticeKind.Success -> DeftActionIcons.Check
                    DeftNoticeKind.Info -> DeftActionIcons.Info
                },
                null,
                Modifier.background(tint.copy(alpha = 0.12f), DeftShapes.small).padding(6.dp).size(20.dp),
                tint = tint,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    notice.message,
                    style = if (actionable) DeftTypography.label else DeftTypography.body,
                    color = colors.text,
                )
                notice.detail?.let { Text(it, style = DeftTypography.monoSmall, color = colors.muted) }
            }
            IconButton(
                dismiss,
                Modifier.size(48.dp),
            ) { Icon(DeftActionIcons.Close, "dismiss notice", tint = colors.muted) }
        }
        if (actionable) {
            HorizontalDivider(color = colors.line)
            TextButton(
                onClick = {
                    dismiss()
                    notice.onAction?.invoke()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                shape = DeftShapes.small,
            ) {
                Text(requireNotNull(notice.actionLabel), style = DeftTypography.label, color = colors.accentText)
                Spacer(Modifier.weight(1f))
                Icon(DeftActionIcons.ChevronRight, null, Modifier.size(20.dp), tint = colors.accentText)
            }
        }
    }
}
