package com.kmab.deft

import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.library.scanner.ScanStatus

internal data class ScanOutcomeNotice(
    val title: String,
    val detail: String?,
    val kind: DeftNoticeKind,
)

internal fun ScanStatus.outcomeNotice(): ScanOutcomeNotice {
    val result = report
    return ScanOutcomeNotice(
        result?.summary()?.substringBefore(" · ") ?: "scan stopped",
        result?.let { "${it.added} added · ${it.updated} updated · ${it.errors} issues" } ?: message,
        if (result == null || result.errors > 0 || result.isPartial) DeftNoticeKind.Error else DeftNoticeKind.Success,
    )
}
