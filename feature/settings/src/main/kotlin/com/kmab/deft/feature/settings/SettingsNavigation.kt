package com.kmab.deft.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.LocalDeftColors

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable("settings") {
        SettingsScreen({ navController.popBackStack() }, { navController.navigate("settings/library") })
    }
    composable("settings/library") {
        LibrarySettingsScreen(onBack = { navController.popBackStack() })
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLibrary: () -> Unit,
) {
    val colors = LocalDeftColors.current
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar("settings", actions = {
            DeftButton("back", onBack, style = DeftButtonStyle.Text)
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
            DeftButton(
                "library",
                onLibrary,
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                style = DeftButtonStyle.Text,
            )
        }
    }
}
