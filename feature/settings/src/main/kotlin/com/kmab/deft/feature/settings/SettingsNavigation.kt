package com.kmab.deft.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable("settings") {
        SettingsScreen({ navController.popBackStack() }, { navController.navigate("settings/library") })
    }
    composable("settings/library") {
        LibrarySettingsScreen(onBack = { navController.popBackStack() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLibrary: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("settings") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("back") }
        })
    }) { padding ->
        Column(androidx.compose.ui.Modifier.padding(padding)) {
            TextButton(onClick = onLibrary) { Text("library") }
        }
    }
}
