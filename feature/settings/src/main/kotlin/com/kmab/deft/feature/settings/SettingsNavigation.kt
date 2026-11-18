package com.kmab.deft.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kmab.deft.feature.readersettings.ReaderSettingsScreen

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onScanDetails: () -> Unit = {},
) {
    composable("settings") {
        SettingsHomeScreen(
            { navController.popBackStack() },
            { navController.navigate("settings/library") },
            { navController.navigate("settings/reader") },
        )
    }
    composable("settings/library") {
        LibrarySettingsScreen(
            onBack = { navController.popBackStack() },
            onScanDetails = onScanDetails,
            onIgnoredBooks = { navController.navigate("settings/library/ignored") },
        )
    }
    composable("settings/reader") {
        ReaderSettingsScreen(onBack = { navController.popBackStack() })
    }
    composable("settings/library/ignored") {
        IgnoredBooksScreen(onBack = { navController.popBackStack() }, onOpenBook = {
            navController.navigate("book/$it")
        })
    }
}
