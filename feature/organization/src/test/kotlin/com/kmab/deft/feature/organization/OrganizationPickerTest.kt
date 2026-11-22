package com.kmab.deft.feature.organization

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizationStats
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
class OrganizationPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun duplicateNamesRemainDistinguishableAndSelectTheExactId() {
        var added: Long? = null
        val categories =
            (1L..2L).map { id ->
                CategorySummary(
                    CategoryEntity(id = id, name = "Earthsea", createdAt = Instant.EPOCH),
                    emptyList(),
                    emptyList(),
                    OrganizationStats(),
                )
            }
        compose.setContent {
            DeftTheme {
                CategoryPickerContent(categories, false, { added = it }, {})
            }
        }
        compose.onNodeWithText("untyped · 0 books · #2").performClick()
        assertEquals(2L, added)
    }
}
