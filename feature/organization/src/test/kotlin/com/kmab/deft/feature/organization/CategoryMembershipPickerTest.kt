package com.kmab.deft.feature.organization

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.library.organization.OrganizationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
class CategoryMembershipPickerTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val temp = TemporaryFolder()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val visible = mutableStateOf(true)
    private lateinit var database: AppDatabase
    private lateinit var repository: OrganizationRepository
    private lateinit var model: OrganizationViewModel
    private var saved = 0

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = OrganizationRepository(database)
        val file = temp.root.resolve("picker.preferences_pb")
        val settings = LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { file })
        model = OrganizationViewModel(repository, settings, CategoryCoverStore(context))
    }

    @After fun cleanup() {
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        val job = compose.runOnIdle { model.viewModelScope.coroutineContext[Job]?.also { it.cancel() } }
        await { job?.isCompleted != false }
        runBlocking { storeScope.coroutineContext[Job]?.cancelAndJoin() }
        database.close()
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun creationStaysVisibleWithManyCategoriesAndNoSearchMatches() {
        seedCategories()
        showPicker()
        compose.onNodeWithText("create category").assertIsDisplayed()
        compose.onNodeWithTag("category_picker_list").performScrollToIndex(35)
        compose.onNodeWithText("create category").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("find a category")).performTextInput("no match")
        compose.onNodeWithText("No matching categories.").assertIsDisplayed()
        compose.onNodeWithText("create category").assertIsDisplayed().performClick()
        compose.onNodeWithText("new category").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w852dp-h393dp-land-mdpi")
    fun createActionIsVisibleInLandscapeWithLargeText() {
        RuntimeEnvironment.setFontScale(1.5f)
        seedCategories()
        showPicker()
        compose.onNodeWithText("create category").assertIsDisplayed()
        capture("045-category-picker-landscape-large")
        compose.onNodeWithText("create category").performClick()
        compose.onNodeWithText("save category").assertIsDisplayed()
        compose.onNodeWithText("cancel").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun creatingCategoryAssignsOnlySelectedBooksAndChosenType() {
        val books = seedBooks()
        val selected = books.take(2).toSet()
        showPicker(selected)
        compose.onNodeWithText("create category").assertIsDisplayed()
        capture("045-category-picker-empty")
        compose.onNodeWithText("create category").performClick()
        compose.onNode(hasSetTextAction() and hasText("name")).performTextInput("Weekend reading")
        compose.onNodeWithText("Series", ignoreCase = true).performScrollTo().performClick()
        compose.onNodeWithText("save category").performClick()
        await { saved == 1 }
        val category = runBlocking { repository.get().categories.single() }
        assertEquals("Weekend reading", category.category.name)
        assertEquals(true, category.isSeries)
        assertEquals(selected, category.books.map { it.membership.bookId }.toSet())
        assertEquals(false, category.books.any { it.membership.suggested })
    }

    @Test fun cancellingCreationKeepsSelectionForExistingCategoryAssignment() {
        val selected = seedBooks().take(2).toSet()
        val existingId = runBlocking { repository.saveCategory(name = "Already here") }
        showPicker(selected)
        compose.onNodeWithText("create category").performClick()
        compose.onNode(hasSetTextAction() and hasText("name")).performTextInput("Discard me")
        compose.onNodeWithText("cancel").performClick()
        compose.onNodeWithText("add to category").assertIsDisplayed()
        assertEquals(0, saved)
        assertEquals(listOf(existingId), runBlocking { repository.get().categories.map { it.category.id } })
        compose.onNodeWithText("Already here").performClick()
        await { saved == 1 }
        assertEquals(
            selected,
            runBlocking {
                repository
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.membership.bookId }
                    .toSet()
            },
        )
    }

    private fun showPicker(selected: Set<Long> = setOf(1L)) {
        compose.setContent {
            if (visible.value) {
                DeftTheme {
                    CategoryMembershipPicker(selected, { visible.value = false }, {
                        saved++
                        visible.value = false
                    }, model)
                }
            }
        }
        await { compose.onAllNodesWithText("find a category").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun seedCategories() = runBlocking { repeat(40) { repository.saveCategory(name = "Category $it") } }

    private fun seedBooks() =
        runBlocking {
            (1..3).map { index ->
                database.bookDao().insert(
                    BookEntity(
                        contentHash = "picker-$index",
                        format = BookFormat.EPUB,
                        title = "Book $index",
                        sortTitle = "Book $index",
                        addedAt = Instant.EPOCH,
                    ),
                )
            }
        }

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            compose.waitForIdle()
            condition()
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(name: String) {
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        val roots = compose.onAllNodes(isRoot())
        roots[roots.fetchSemanticsNodes().lastIndex].captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
