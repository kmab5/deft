package com.kmab.deft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeftTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = LocalDeftColors.current.bg) {
                    NavHost(
                        rememberNavController(),
                        startDestination = "books",
                        modifier = Modifier.safeDrawingPadding(),
                    ) {
                        composable("books") { Text("Deft") }
                    }
                }
            }
        }
    }
}
