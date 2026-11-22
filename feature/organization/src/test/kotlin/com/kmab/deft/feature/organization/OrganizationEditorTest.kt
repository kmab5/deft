package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookState
import com.kmab.deft.library.organization.AuthorSummary
import com.kmab.deft.library.organization.CategoryBook
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizationRepository
import com.kmab.deft.library.organization.OrganizationSnapshot
import com.kmab.deft.library.organization.OrganizationStats
import com.kmab.deft.library.organization.OrganizedBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationEditorTest {
    @get:Rule(order = 0)
    val mainDispatcher =
        object : ExternalResource() {
            override fun before() {
                Dispatchers.setMain(UnconfinedTestDispatcher())
            }

            override fun after() {
                Dispatchers.resetMain()
            }
        }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @get:Rule val temp = TemporaryFolder()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var repository: OrganizationRepository
    private lateinit var model: OrganizationViewModel

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = OrganizationRepository(database)
        val file = temp.root.resolve("organization.preferences_pb")
        val settings = LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { file })
        model = OrganizationViewModel(repository, settings, CategoryCoverStore(context))
    }

    @After fun cleanup() {
        model.viewModelScope.cancel()
        storeScope.cancel()
        database.close()
    }

    @Test fun createEditorPersistsDescriptionAndChosenSeriesType() {
        val saved = AtomicBoolean()
        val types = runBlocking { repository.get().types }
        compose.setContent {
            DeftTheme { CategoryEditor(null, types, model, {}, { saved.set(true) }) }
        }
        compose.onNode(hasSetTextAction() and hasText("name")).performTextInput("Earthsea")
        compose.onNode(hasSetTextAction() and hasText("description")).performTextInput("Island stories")
        compose.onNodeWithText("Series", ignoreCase = true).performScrollTo().performClick()
        compose.onNodeWithText("save category").performClick()
        awaitMutation(saved)
        val category = runBlocking { repository.get().categories.single() }
        assertEquals("Earthsea", category.category.name)
        assertEquals("Island stories", category.category.description)
        assertEquals(true, category.isSeries)
    }

    @Test fun selectingMergeTargetRequiresConfirmationBeforeSourceDisappears() {
        val merged = AtomicBoolean()
        val (sourceId, targetId) =
            runBlocking {
                repository.saveCategory(name = "Series draft") to repository.saveCategory(name = "Earthsea")
            }
        val categories = runBlocking { repository.get().categories }
        val source = categories.single { it.category.id == sourceId }
        compose.setContent {
            DeftTheme { CategoryMergeSheet(source, categories, model, {}, { merged.set(true) }) }
        }
        compose.onNodeWithText("Earthsea").performClick()
        compose.onNodeWithText("merge categories?").assertExists()
        assertFalse(merged.get())
        assertEquals(2, runBlocking { repository.get().categories.size })
        compose.onNodeWithText("merge").performClick()
        awaitMutation(merged)
        assertEquals(
            targetId,
            runBlocking {
                repository
                    .get()
                    .categories
                    .single()
                    .category.id
            },
        )
    }

    @Test fun duplicateTypeMutationDisplaysNoticeInsideSheet() {
        val types =
            runBlocking {
                repository.saveType(name = "Classics")
                repository.get().types
            }
        compose.setContent {
            DeftTheme { CategoryTypes(types, model, {}) }
        }
        compose.onNode(hasSetTextAction() and hasText("new type name")).performTextInput("classics")
        compose.onNodeWithText("create type").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("A type with this name already exists.").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(2, runBlocking { repository.get().types.size })
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun paperCategoryDetailShowsMissingMetadataAndOpensTheMember() {
        val series = CategoryTypeEntity(1, "Series", "series", true)
        val record = CategoryEntity(7, "Island stories", description = "A collection", createdAt = Instant.EPOCH)
        val book = detailBook("  ", "  ")
        val member = CategoryBook(book, CategoryMemberEntity(7, book.book.id, 2.5, true, Instant.EPOCH))
        val category = CategorySummary(record, listOf(series), listOf(member), OrganizationStats(total = 1))
        val snapshot = OrganizationSnapshot(categories = listOf(category), types = listOf(series), books = listOf(book))
        var opened = 0L
        compose.setContent {
            DeftTheme(darkTheme = false) {
                OrganizationRenderSurface {
                    Column {
                        DeftTopBar("category")
                        CategoryDetail(category, snapshot, model, {}) { opened = it }
                    }
                }
            }
        }
        val membership =
            compose.onNode(
                hasText("<no-title>") and hasText("#2.5 · <no-author> · file missing · suggested"),
            )
        membership.assertExists()
        captureDetail("category-detail-paper")
        membership.performClick()
        assertEquals(book.book.id, opened)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun inkAuthorDetailShowsProgressTimeAndGroupedBookAction() {
        val stats = OrganizationStats(total = 1, finished = 1, activeSeconds = 3_660, averageRating = 4.5)
        val book =
            detailBook("A Wizard of Earthsea", "Ursula K. Le Guin").copy(
                state = BookState.FINISHED,
                rating = 4.5,
                activeSeconds = 3_660,
            )
        val author =
            AuthorSummary(
                AuthorEntity(8, "Ursula K. Le Guin", "Le Guin", "ursula k. le guin"),
                listOf(book),
                stats,
            )
        var opened = 0L
        compose.setContent {
            DeftTheme(darkTheme = true) {
                OrganizationRenderSurface {
                    Column {
                        DeftTopBar("author")
                        AuthorDetail(author, listOf(author), model, {}) { opened = it }
                    }
                }
            }
        }
        compose.onNodeWithText("1 h 1 min read").assertExists()
        compose.onNodeWithText("average rating · 4.5").assertExists()
        captureDetail("author-detail-ink")
        compose.onNodeWithText(book.book.title).performClick()
        assertEquals(book.book.id, opened)
    }

    @Test fun addBooksConfirmationStaysVisibleAfterScrollingAndFiltering() {
        val books =
            (1L..40L).map { id ->
                detailBook("Book $id", "Author $id").let { it.copy(book = it.book.copy(id = id)) }
            }
        val category =
            CategorySummary(
                CategoryEntity(7, "Reading list", createdAt = Instant.EPOCH),
                emptyList(),
                emptyList(),
                OrganizationStats(),
            )
        compose.setContent { DeftTheme { CategoryAddBooks(category, books, model, {}) } }
        compose.onNodeWithText("Book 1").performClick()
        compose.onNodeWithTag("category_add_books_list").performScrollToIndex(30)
        compose.onNodeWithContentDescription("add 1 selected books").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("find books")).performTextInput("nothing matches")
        compose.onNodeWithContentDescription("add 1 selected books").assertIsDisplayed()
    }

    @Test fun categoryMemberGridAndSeriesReorderAreVisibleActions() {
        val series = CategoryTypeEntity(1, "Series", "series", true)
        val record = CategoryEntity(7, "Series", createdAt = Instant.EPOCH)
        val books =
            (1L..2L).map { id ->
                detailBook("Book $id", "Author").let { it.copy(book = it.book.copy(id = id)) }
            }
        val members =
            books.map {
                CategoryBook(it, CategoryMemberEntity(7, it.book.id, it.book.id.toDouble(), false, Instant.EPOCH))
            }
        val category = CategorySummary(record, listOf(series), members, OrganizationStats(total = 2))
        val snapshot = OrganizationSnapshot(categories = listOf(category), types = listOf(series), books = books)
        compose.setContent {
            DeftTheme {
                OrganizationRenderSurface {
                    CategoryDetail(
                        category,
                        snapshot,
                        model,
                        {},
                    ) {}
                }
            }
        }
        compose.onNodeWithText("reorder books").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("show book grid").performClick()
        compose.onNodeWithContentDescription("show book list").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun renderInkCategoryEditorWithAnchoredActions() {
        RuntimeEnvironment.setFontScale(1.5f)
        val types = runBlocking { repository.get().types }
        compose.setContent { DeftTheme(darkTheme = true) { CategoryEditor(null, types, model, {}) } }
        compose.onNode(hasSetTextAction() and hasText("name")).performTextInput("Weekend reading")
        captureSheet("category-editor-ink-large")
        compose.onNodeWithText("save category").assertIsDisplayed()
        compose.onNodeWithText("cancel").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun renderPaperAddBooksWithSelectedConfirmation() {
        RuntimeEnvironment.setFontScale(1.5f)
        val books =
            (1L..12L).map { id ->
                detailBook("Book $id", "Ursula K. Le Guin").let {
                    it.copy(book = it.book.copy(id = id, fileLocation = "Fiction/Earthsea/Book $id.epub"))
                }
            }
        val category =
            CategorySummary(
                CategoryEntity(7, "Reading list", createdAt = Instant.EPOCH),
                emptyList(),
                emptyList(),
                OrganizationStats(),
            )
        compose.setContent { DeftTheme(darkTheme = false) { CategoryAddBooks(category, books, model, {}) } }
        compose.onNodeWithText("Book 1").performClick()
        captureSheet("category-add-books-paper-large")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun renderPaperCategoryBrowserWithGridAndSeparateControls() {
        val books =
            (1L..2L).map { id ->
                detailBook("A Wizard of Earthsea", "Ursula K. Le Guin").let { it.copy(book = it.book.copy(id = id)) }
            }
        val categories =
            books.map { book ->
                CategorySummary(
                    CategoryEntity(
                        book.book.id,
                        if (book.book.id ==
                            1L
                        ) {
                            "Earthsea"
                        } else {
                            "Weekend reading"
                        },
                        createdAt = Instant.EPOCH,
                    ),
                    emptyList(),
                    listOf(
                        CategoryBook(book, CategoryMemberEntity(book.book.id, book.book.id, 1.0, false, Instant.EPOCH)),
                    ),
                    OrganizationStats(total = 1),
                )
            }
        val state =
            OrganizationUiState(
                OrganizationSnapshot(categories = categories, books = books),
                LibrarySettings(categoryGridView = true),
            )
        val browser = mutableStateOf(state)
        compose.setContent {
            DeftTheme(darkTheme = false) { OrganizationRenderSurface { CategoriesScreen(browser.value, model) {} } }
        }
        captureDetail("categories-browser-paper")
        compose.runOnIdle { browser.value = state.copy(preferences = state.preferences.copy(categoryGridView = false)) }
        captureDetail("categories-list-paper")
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureSheet(name: String) {
        val image = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        val roots = compose.onAllNodes(isRoot())
        roots[roots.fetchSemanticsNodes().lastIndex].captureRoboImage(
            image.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    private fun detailBook(
        title: String,
        authors: String,
    ) = OrganizedBook(
        BookListItem(
            id = 11,
            title = title,
            sortTitle = title,
            authorsDisplay = authors,
            format = BookFormat.EPUB,
            coverPath = null,
            addedAt = Instant.EPOCH,
            missing = true,
            drmProtected = false,
        ),
    )

    private fun awaitMutation(completed: AtomicBoolean) {
        try {
            compose.waitUntil(10_000) { completed.get() }
        } catch (timeout: ComposeTimeoutException) {
            val tree = compose.onAllNodes(isRoot(), useUnmergedTree = true).printToString()
            throw AssertionError(
                "Mutation timed out (busy=${model.busy.value}, error=${model.error.value})\n$tree",
                timeout,
            )
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureDetail(name: String) {
        val image = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            image.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
