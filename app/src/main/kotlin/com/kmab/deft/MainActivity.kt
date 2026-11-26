package com.kmab.deft

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.kmab.deft.core.datastore.AppearanceSettingsRepository
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
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

    @Inject lateinit var appearanceSettings: AppearanceSettingsRepository

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
            val appTheme by appearanceSettings.theme.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = appTheme?.isDark(isSystemInDarkTheme()) ?: return@setContent
            DeftTheme(darkTheme = darkTheme) {
                val lightBars = LocalDeftColors.current.bg.luminance() >= LIGHT_BARS_LUMINANCE
                LaunchedEffect(lightBars) {
                    val bars =
                        if (lightBars) {
                            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        }
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                val completedBook by readerLauncher.pendingCompletion.collectAsStateWithLifecycle()
                DeftApp(
                    scanStatus,
                    scanReports,
                    readerLauncher::open,
                    readerLauncher::openAt,
                    completedBook,
                    readerLauncher::acknowledgeCompletion,
                )
            }
        }
    }

    private companion object {
        // Black icons have greater contrast than white icons above this background luminance.
        const val LIGHT_BARS_LUMINANCE = 0.179f
    }
}
