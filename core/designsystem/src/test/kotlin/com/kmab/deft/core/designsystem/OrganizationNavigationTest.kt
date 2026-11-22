package com.kmab.deft.core.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrganizationNavigationTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val temporary = TemporaryFolder()

    @Test fun onlyWorkingDestinationsAreVisibleAndKeepTheirIndices() {
        var selected = -1
        compose.setContent { DeftTheme { DeftBottomBar(0, { selected = it }, destinations = listOf(0, 1, 2)) } }
        compose.onNodeWithText("notes").assertDoesNotExist()
        compose.onNodeWithText("stats").assertDoesNotExist()
        compose.onNodeWithText("authors").performClick()
        assertEquals(2, selected)
        compose.onNodeWithText("categories").performClick()
        assertEquals(1, selected)
    }

    @Test fun coverPathsCannotEscapePrivateStorageOrBecomeRemoteRequests() {
        val root = temporary.newFolder("private")
        val cover = root.resolve("cover.png")
        cover.writeBytes(byteArrayOf(1))
        assertEquals(cover.canonicalFile, privateCoverFile(cover.path, root))
        assertNull(privateCoverFile(root.resolve("../outside.jpg").path, root))
        assertNull(privateCoverFile("https://example.com/cover.jpg", root))
        assertNull(privateCoverFile("content://provider/image", root))
    }
}
