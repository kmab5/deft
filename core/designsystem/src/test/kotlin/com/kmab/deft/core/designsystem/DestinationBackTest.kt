package com.kmab.deft.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DestinationBackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var controller: NavHostController
    private val backActions = mutableMapOf<String, () -> Unit>()

    @Test fun repeatedOutgoingToolbarBackPreservesParentAndRoot() {
        setup()
        navigate("settings")
        navigate("settings/library")
        val outgoing = backActions.getValue("settings/library")
        compose.runOnIdle { repeat(30) { outgoing() } }
        compose.runOnIdle { assertEquals("settings", controller.currentDestination?.route) }
        compose.runOnIdle { repeat(30) { backActions.getValue("settings")() } }
        compose.runOnIdle {
            outgoing()
            assertEquals("books", controller.currentDestination?.route)
            assertEquals(null, controller.previousBackStackEntry)
        }
    }

    @Test fun repeatedSystemBackNeverEmptiesGraph() {
        setup()
        navigate("book/1")
        compose.runOnIdle { repeat(30) { compose.activity.onBackPressedDispatcher.onBackPressed() } }
        compose.runOnIdle { assertEquals("books", controller.currentDestination?.route) }
    }

    @Test fun backgroundAndStaleBookCallbacksCannotPopNewDestination() {
        setup()
        navigate("book/1")
        val oldBook = backActions.getValue("book/1")
        navigate("settings")
        compose.runOnIdle {
            oldBook()
            assertEquals("settings", controller.currentDestination?.route)
        }
    }

    @Test fun rootCannotBePoppedEvenByProgrammaticCompletion() {
        setup()
        compose.runOnIdle {
            val root = requireNotNull(controller.currentBackStackEntry)
            assertEquals(false, controller.popDestination(root))
            assertEquals("books", controller.currentDestination?.route)
        }
    }

    private fun navigate(route: String) {
        compose.runOnIdle { controller.navigate(route) }
        compose.waitForIdle()
    }

    private fun setup() {
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = "books") {
                composable("books") { Text("books") }
                listOf("book/1", "settings", "settings/library").forEach { route ->
                    composable(route) { entry ->
                        backActions[route] = rememberDestinationBack(controller, entry)
                        Text(route)
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
