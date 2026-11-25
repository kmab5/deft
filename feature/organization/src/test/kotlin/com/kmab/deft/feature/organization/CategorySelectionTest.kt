package com.kmab.deft.feature.organization

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.library.organization.OrganizationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@OptIn(ExperimentalCoroutinesApi::class)
class CategorySelectionTest {
    @get:Rule(order = 0)
    val dispatcher =
        object : ExternalResource() {
            override fun before() = Dispatchers.setMain(UnconfinedTestDispatcher())

            override fun after() = Dispatchers.resetMain()
        }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val temp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rendered = mutableStateOf(true)
    private lateinit var database: AppDatabase
    private lateinit var repository: OrganizationRepository
    private lateinit var settings: LibrarySettingsRepository
    private lateinit var model: OrganizationViewModel
    private var alpha = 0L
    private var beta = 0L
    private var gamma = 0L
    private var opened: Long? = null

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = OrganizationRepository(database)
        settings =
            LibrarySettingsRepository(
                PreferenceDataStoreFactory.create(scope = scope) {
                    temp.root.resolve("prefs.preferences_pb")
                },
            )
        model = OrganizationViewModel(repository, settings, CategoryCoverStore(context))
        runBlocking {
            settings.setCategoryGridView(false)
            settings.setGroupCategoriesByType(false)
            alpha = repository.saveCategory(name = "Alpha")
            beta = repository.saveCategory(name = "Beta")
            gamma = repository.saveCategory(name = "Gamma")
        }
    }

    @After fun cleanup() {
        compose.runOnIdle { rendered.value = false }
        compose.waitForIdle()
        runBlocking {
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
        database.close()
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun show(dark: Boolean = false): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val state = model.state.collectAsState().value
            if (rendered.value) {
                DeftTheme(darkTheme = dark) {
                    OrganizationRenderSurface { CategoriesScreen(state, model) { opened = it } }
                }
            }
        }
        compose.waitUntil {
            model.state.value.snapshot
                ?.categories
                ?.size == 3
        }
        compose.waitForIdle()
        return restoration
    }

    private fun selectAlpha() {
        compose.onNodeWithText("Alpha").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithContentDescription("1 selected").assertIsDisplayed()
        assertEquals(null, opened)
    }

    @Test fun longPressTogglesSelectionAndBackClearsBeforeOpening() {
        show()
        selectAlpha()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithContentDescription("2 selected").assertIsDisplayed()
        compose.onNodeWithTag("category_$alpha").assertIsSelected()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("category_selection_bar").assertDoesNotExist()
        compose.onNodeWithText("Beta").performClick()
        assertEquals(beta, opened)
    }

    @Test fun selectionSurvivesRestorationAndFiltersToVisibleResults() {
        val restoration = show()
        selectAlpha()
        compose.onNodeWithContentDescription("select all").performClick()
        compose.onNodeWithContentDescription("3 selected").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("3 selected").assertIsDisplayed()
        compose.onNodeWithText("find categories").performTextInput("Alpha")
        compose.onNodeWithContentDescription("1 selected").assertIsDisplayed()
        compose.onNodeWithTag("category_$alpha").assertIsSelected()
    }

    @Test fun groupedCategoriesSelectAllByIdentityInsteadOfAppearance() {
        runBlocking {
            val first = repository.saveType(name = "First")
            val second = repository.saveType(name = "Second")
            repository.saveCategory(alpha, "Alpha", typeIds = setOf(first, second))
            settings.setGroupCategoriesByType(true)
        }
        show()
        compose.onAllNodesWithTag("category_$alpha")[0].performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithContentDescription("select all").performClick()
        compose.onNodeWithContentDescription("3 selected").assertIsDisplayed()
    }

    @Test fun deleteCanBeCancelledThenRemovesOnlySelectedCategories() {
        show()
        selectAlpha()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("delete categories").performClick()
        compose.onNodeWithText("cancel").performClick()
        compose.onNodeWithContentDescription("2 selected").assertIsDisplayed()
        assertEquals(3, runBlocking { repository.get().categories.size })
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("delete categories").performClick()
        compose.onNodeWithText("delete").performClick()
        compose.waitForIdle()
        compose.waitUntil { runBlocking { repository.get().categories.size == 1 } }
        compose.onNodeWithTag("category_selection_bar").assertDoesNotExist()
        assertEquals(
            gamma,
            runBlocking {
                repository
                    .get()
                    .categories
                    .single()
                    .category.id
            },
        )
    }

    @Test fun bulkTypesApplyOnlyToSelectedCategories() {
        show()
        selectAlpha()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("add types").performClick()
        compose.onNodeWithText("Series").performClick()
        compose.onNodeWithText("apply types").performClick()
        compose.waitUntil { runBlocking { repository.get().categories.count { it.isSeries } == 2 } }
        assertEquals(
            false,
            runBlocking {
                repository
                    .get()
                    .categories
                    .single { it.category.id == gamma }
                    .isSeries
            },
        )
        compose.onNodeWithTag("category_selection_bar").assertDoesNotExist()
    }

    @Test fun mergeUsesExplicitDestinationAndCanBeCancelled() {
        show()
        selectAlpha()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("merge categories").performClick()
        compose.onNodeWithTag("merge_target_$gamma").performClick()
        compose.onNodeWithText("cancel").performClick()
        assertEquals(3, runBlocking { repository.get().categories.size })
        compose.onNodeWithTag("merge_target_$gamma").performClick()
        compose.onNodeWithText("merge").performClick()
        compose.waitUntil { runBlocking { repository.get().categories.size == 1 } }
        assertEquals(
            gamma,
            runBlocking {
                repository
                    .get()
                    .categories
                    .single()
                    .category.id
            },
        )
        compose.onNodeWithTag("category_selection_bar").assertDoesNotExist()
    }

    @Test fun removeTypesKeepsUnrelatedTypesAndClearsOnlyAfterSuccess() {
        val other =
            runBlocking {
                val type = repository.saveType(name = "Keep")
                repository.saveCategory(alpha, "Alpha", typeIds = setOf(1L, type))
                type
            }
        show()
        selectAlpha()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("remove types").performClick()
        compose.onNodeWithText("Series").performClick()
        compose.onNodeWithText("apply types").performClick()
        compose.waitUntil {
            !model.busy.value &&
                runBlocking {
                    !repository
                        .get()
                        .categories
                        .first { it.category.id == alpha }
                        .isSeries
                }
        }
        assertEquals(
            listOf(other),
            runBlocking {
                repository
                    .get()
                    .categories
                    .first { it.category.id == alpha }
                    .types
                    .map { it.id }
            },
        )
        compose.onNodeWithTag("category_selection_bar").assertDoesNotExist()
    }

    @Test fun databaseFailureRetainsSelectionAndCategories() {
        runBlocking {
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER refuse_delete BEFORE DELETE ON categories BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
        }
        show()
        selectAlpha()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("delete categories").performClick()
        compose.onNodeWithText("delete").performClick()
        compose.waitForIdle()
        compose.waitUntil { model.error.value != null && !model.busy.value }
        compose.onNodeWithContentDescription("1 selected").assertIsDisplayed()
        assertEquals(3, runBlocking { repository.get().categories.size })
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w320dp-h780dp-mdpi")
    fun narrowLargeTextSelectionAndTypesRemainUsable() {
        RuntimeEnvironment.setFontScale(1.5f)
        show()
        selectAlpha()
        capture("044-selection-paper-large")
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("add types").performClick()
        compose.onNodeWithText("Series").assertIsDisplayed()
        compose.onNodeWithText("apply types").assertIsDisplayed()
        capture("044-types-paper-large")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w852dp-h393dp-land-mdpi")
    fun inkLandscapeSelectionAndMergeRemainUsable() {
        runBlocking { settings.setCategoryGridView(true) }
        show(dark = true)
        selectAlpha()
        capture("044-selection-ink-landscape")
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("merge categories").performClick()
        compose.onNodeWithTag("merge_target_$beta").assertIsDisplayed()
        capture("044-merge-ink-landscape")
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-mdpi")
    fun largeTextLandscapeConfirmationButtonsAreReachable() {
        RuntimeEnvironment.setFontScale(1.5f)
        show()
        selectAlpha()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("delete categories").performClick()
        compose.onNodeWithText("delete").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("cancel").performClick()
        compose.onNodeWithContentDescription("1 selected").assertIsDisplayed()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(name: String) {
        val file = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            file.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    @Test fun completionAfterRecreationClearsRestoredSelectionAndSheet() {
        val restoration = show()
        selectAlpha()
        compose.onNodeWithContentDescription("category selection actions").performClick()
        compose.onNodeWithText("add types").performClick()
        compose.onNodeWithText("Series").performClick()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blocker =
            scope.launch {
                database.withTransaction {
                    entered.complete(Unit)
                    release.await()
                }
            }
        runBlocking { entered.await() }
        try {
            compose.onNodeWithText("apply types").performClick()
            compose.waitForIdle()
            assertEquals(true, model.busy.value)
            restoration.emulateSavedInstanceStateRestore()
        } finally {
            release.complete(Unit)
        }
        compose.waitUntil { !model.busy.value }
        runBlocking { blocker.join() }
        compose.onNodeWithTag("category_selection_bar").assertDoesNotExist()
        compose.onNodeWithText("apply types").assertDoesNotExist()
    }
}
