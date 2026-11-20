package com.kmab.deft.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kmab.deft.core.designsystem.rememberDestinationBack
import com.kmab.deft.feature.readersettings.ReaderSettingsScreen

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onScanDetails: () -> Unit = {},
) {
    composable("settings") { entry ->
        val onBack = rememberDestinationBack(navController, entry)
        SettingsHomeScreen(
            onBack,
            { navController.navigate("settings/library") },
            { navController.navigate("settings/reader") },
            { navController.navigate("settings/tracking") },
            { navController.navigate("settings/appearance") },
        )
    }
    composable("settings/tracking") { entry ->
        TrackingSettingsScreen(rememberDestinationBack(navController, entry))
    }
    composable("settings/appearance") { entry ->
        TrackingSettingsScreen(rememberDestinationBack(navController, entry), appearance = true)
    }
    composable("settings/library") { entry ->
        LibrarySettingsScreen(
            onBack = rememberDestinationBack(navController, entry),
            onScanDetails = onScanDetails,
            onIgnoredBooks = { navController.navigate("settings/library/ignored") },
        )
    }
    composable("settings/reader") { entry ->
        ReaderSettingsScreen(onBack = rememberDestinationBack(navController, entry))
    }
    composable("settings/library/ignored") { entry ->
        IgnoredBooksScreen(onBack = rememberDestinationBack(navController, entry), onOpenBook = {
            navController.navigate("book/$it")
        })
    }
}
