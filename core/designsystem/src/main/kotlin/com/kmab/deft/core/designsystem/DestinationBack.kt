package com.kmab.deft.core.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/** Outgoing destinations can remain clickable during their exit transition. */
fun NavController.popDestination(entry: NavBackStackEntry): Boolean {
    if (currentBackStackEntry != entry || previousBackStackEntry == null) return false
    return popBackStack()
}

@Composable
fun rememberDestinationBack(
    controller: NavController,
    entry: NavBackStackEntry,
): () -> Unit {
    val back: () -> Unit =
        remember(controller, entry) {
            {
                if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    controller.popDestination(entry)
                }
            }
        }
    // Consume repeated system Back during transitions, just like the toolbar action.
    BackHandler(onBack = back)
    return back
}
