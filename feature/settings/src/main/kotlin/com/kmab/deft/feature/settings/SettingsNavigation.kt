package com.kmab.deft.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable("settings") {
        LibrarySettingsScreen(onBack = { navController.popBackStack() })
    }
    composable("settings/library") {
        LibrarySettingsScreen(onBack = { navController.popBackStack() })
    }
}
