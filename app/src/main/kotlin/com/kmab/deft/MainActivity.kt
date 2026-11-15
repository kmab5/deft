package com.kmab.deft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.library.scanner.ScanScheduler
import com.kmab.deft.feature.library.libraryGraph
import com.kmab.deft.feature.settings.settingsGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var scanScheduler: ScanScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { scanScheduler.scanOnStartIfStale() }
        enableEdgeToEdge()
        setContent {
            DeftTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = LocalDeftColors.current.bg) {
                    val navController = rememberNavController()
                    NavHost(
                        navController,
                        startDestination = "books",
                        modifier = Modifier.safeDrawingPadding(),
                    ) {
                        libraryGraph(navController)
                        settingsGraph(navController)
                    }
                }
            }
        }
    }
}
