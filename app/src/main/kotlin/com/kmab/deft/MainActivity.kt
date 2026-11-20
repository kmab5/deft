package com.kmab.deft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.feature.reader.ReaderLauncher
import com.kmab.deft.library.scanner.ScanReportStore
import com.kmab.deft.library.scanner.ScanScheduler
import com.kmab.deft.library.scanner.ScanStatusObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var readerLauncher: ReaderLauncher

    @Inject lateinit var scanScheduler: ScanScheduler

    @Inject lateinit var scanStatus: ScanStatusObserver

    @Inject lateinit var scanReports: ScanReportStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { scanScheduler.scanOnStartIfStale() }
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val lastBook =
                    runCatching {
                        readerLauncher.startupBook()
                    }.getOrElse {
                        if (it is CancellationException) throw it
                        null
                    }
                if (lastBook != null) lifecycle.withResumed { readerLauncher.open(lastBook) }
            }
        }
        enableEdgeToEdge()
        setContent {
            DeftTheme {
                DeftApp(scanStatus, scanReports, readerLauncher::open)
            }
        }
    }
}
