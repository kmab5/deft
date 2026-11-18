package com.kmab.deft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.rememberDeftNoticeState
import com.kmab.deft.feature.library.ScanDetailsSheet
import com.kmab.deft.feature.library.libraryGraph
import com.kmab.deft.feature.settings.settingsGraph
import com.kmab.deft.library.scanner.ScanDetails
import com.kmab.deft.library.scanner.ScanReport
import com.kmab.deft.library.scanner.ScanReportStore
import com.kmab.deft.library.scanner.ScanStatus
import com.kmab.deft.library.scanner.ScanStatusObserver

private val NoticeMemorySaver =
    listSaver<ScanNoticeMemory, String>(
        save = { listOf(it.initialized.toString(), it.handledId.orEmpty()) },
        restore = { ScanNoticeMemory(it[0].toBoolean(), it[1].ifEmpty { null }) },
    )

@Composable
internal fun DeftApp(
    observer: ScanStatusObserver,
    reports: ScanReportStore,
) {
    val notices = rememberDeftNoticeState()
    val scan by observer.status.collectAsStateWithLifecycle(ScanStatus())
    var memory by rememberSaveable(stateSaver = NoticeMemorySaver) { mutableStateOf(ScanNoticeMemory()) }
    var selectedReport by remember { mutableStateOf<ReportSelection?>(null) }
    var outcomeNotice by remember { mutableStateOf<Long?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val listener =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) memory = memory.endForegroundSession()
            }
        lifecycle.addObserver(listener)
        onDispose { lifecycle.removeObserver(listener) }
    }
    LaunchedEffect(scan) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@LaunchedEffect
        val decision = scanNoticeDecision(memory, scan, System.currentTimeMillis())
        memory = decision.memory
        if (scan.isScanning) outcomeNotice?.let(notices::dismiss)
        if (decision.show) {
            val report = scan.report
            val selection = scan.reportSelection()
            outcomeNotice =
                notices.show(
                    scan.message ?: report?.let { "Scan complete · ${it.added} added" } ?: "Scan finished",
                    if (report == null || report.errors > 0) DeftNoticeKind.Error else DeftNoticeKind.Success,
                    actionLabel = "scan details",
                    onAction = { selectedReport = selection },
                )
        }
    }
    CompositionLocalProvider(LocalDeftNoticeState provides notices) {
        Surface(Modifier.fillMaxSize(), color = LocalDeftColors.current.bg) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                val navController = rememberNavController()
                val openDetails: () -> Unit = {
                    if (scan.isFinished || scan.report != null) {
                        selectedReport = scan.reportSelection()
                    }
                }
                NavHost(navController, startDestination = "books") {
                    libraryGraph(navController, onScanDetails = openDetails)
                    settingsGraph(navController, onScanDetails = openDetails)
                }
                DeftNoticeHost(notices, Modifier.align(Alignment.BottomCenter), paused = selectedReport != null)
            }
        }
        selectedReport?.let { report -> ReportPopup(report, reports) { selectedReport = null } }
    }
}

@Composable
private fun ReportPopup(
    selection: ReportSelection,
    reports: ScanReportStore,
    onDismiss: () -> Unit,
) {
    var details by remember(selection.id) { mutableStateOf<ScanDetails?>(null) }
    var loading by remember(selection.id) { mutableStateOf(true) }
    LaunchedEffect(selection.id) {
        if (selection.id == null) {
            loading = false
        } else {
            reports.observe(selection.id).collect {
                details = it
                loading = false
            }
        }
    }
    ScanDetailsSheet(selection.report, details, onDismiss, isLoading = loading)
}

private data class ReportSelection(
    val report: ScanReport?,
    val id: String?,
)

private fun ScanStatus.reportSelection(): ReportSelection =
    ReportSelection(report, if (report != null) report?.reportId else workId)
