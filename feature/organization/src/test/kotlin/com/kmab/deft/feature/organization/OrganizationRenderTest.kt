package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.library.organization.AuthorSummary
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizationSnapshot
import com.kmab.deft.library.organization.OrganizationStats
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class OrganizationRenderTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetFont() = RuntimeEnvironment.setFontScale(1f)

    @Test fun paperCategoryShowsRatingOverrideAndOpensExactCategory() {
        var opened = 0L
        compose.setContent {
            DeftTheme(darkTheme = false) {
                OrganizationRenderSurface {
                    Column(Modifier.padding(16.dp)) {
                        CategoryCard(earthsea, grid = false) { opened = earthsea.category.id }
                        val computed =
                            earthsea.copy(
                                category =
                                    earthsea.category.copy(
                                        name = "Earthsea books",
                                        ratingOverride = null,
                                    ),
                            )
                        CategoryCard(computed, grid = false) {}
                    }
                }
            }
        }
        compose.onNodeWithText("2/4 finished · ★ 4.5 · yours").assertExists()
        compose.onNodeWithText("2/4 finished · ★ 3.8").assertExists()
        capture("category-paper")
        compose.onNodeWithText("Earthsea").performClick()
        assertEquals(earthsea.category.id, opened)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-mdpi")
    fun inkAuthorListRemainsActionableAtLargeTextInLandscape() {
        RuntimeEnvironment.setFontScale(1.5f)
        var opened = 0L
        val entry =
            AuthorSummary(
                AuthorEntity(9, "Ursula K. Le Guin", "Le Guin", "ursula k. le guin"),
                emptyList(),
                OrganizationStats(total = 4, finished = 2),
            )
        compose.setContent {
            DeftTheme(darkTheme = true) {
                OrganizationRenderSurface {
                    AuthorsScreen(OrganizationUiState(OrganizationSnapshot(authors = listOf(entry)))) { opened = it }
                }
            }
        }
        compose.onNodeWithText("4 books · 2 finished").assertExists()
        capture("authors-ink-landscape-large")
        compose.onNodeWithText(entry.author.name).performClick()
        assertEquals(9L, opened)
    }

    private fun capture(name: String) {
        val file = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            file.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    private val earthsea =
        CategorySummary(
            CategoryEntity(13, "Earthsea", ratingOverride = 4.5, createdAt = Instant.EPOCH),
            listOf(CategoryTypeEntity(1, "Series", "series", true)),
            emptyList(),
            OrganizationStats(total = 4, finished = 2, reading = 1, averageRating = 3.75),
        )
}

@Composable
internal fun OrganizationRenderSurface(content: @Composable () -> Unit) {
    val colors = LocalDeftColors.current
    Surface(Modifier.fillMaxSize(), color = colors.bg, contentColor = colors.text, content = content)
}
