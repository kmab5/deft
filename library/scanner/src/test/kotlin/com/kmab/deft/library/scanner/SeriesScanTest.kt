package com.kmab.deft.library.scanner

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.common.ContentHasher
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.model.ContentHash
import com.kmab.deft.format.epub.EpubMetadataExtractor
import com.kmab.deft.format.epub.TestEpubBuilder
import com.kmab.deft.library.organization.OrganizationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SeriesScanTest {
    @get:Rule val temp = TemporaryFolder()
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tree = FakeDocumentTree()
    private val organization = OrganizationRepository(db)
    private val settings by lazy {
        val file = temp.newFolder().resolve("library.preferences_pb")
        LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }
    private val clock = TimeSource { Instant.EPOCH }
    private val scanner =
        LibraryScanner(
            tree,
            ContentHasher(),
            MetadataExtractorRegistry(setOf(EpubMetadataExtractor())),
            object : CoverStore {
                override suspend fun save(
                    hash: ContentHash,
                    image: ByteArray,
                ): String? = null
            },
            db,
            clock,
        )
    private val runner by lazy {
        ScanRunner(db, scanner, settings, clock, ScanCoordination(), MemoryFolderAccess(), organization)
    }

    @After fun close() {
        scope.cancel()
        db.close()
    }

    @Test fun organizationFailureKeepsImportedBooksAndDoesNotMarkScanComplete() =
        runBlocking {
            db.libraryFolderDao().insert(LibraryFolderEntity("R", "Books", Instant.EPOCH))
            organization.get()
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_series BEFORE INSERT ON categories BEGIN SELECT RAISE(ABORT, 'test failure'); END",
            )
            tree.put("one", bytes = TestEpubBuilder().title("One").calibreSeries("Series", 1.0).build())
            try {
                runner.run()
                error("Expected a reported organization failure")
            } catch (failure: SeriesOrganizationException) {
                assertTrue(failure.message!!.contains("series couldn't be organized"))
            }
            assertEquals(1, db.bookFileDao().getAll().size)
            assertNull(settings.settings.first().lastScanAt)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_series")
            runner.run()
            assertEquals(
                1,
                organization
                    .get()
                    .categories
                    .single()
                    .books.size,
            )
        }

    @Test fun realEpubSeriesAutoFillRespectsDisableMergeAliasesAndIgnoredBooks() =
        runBlocking {
            db.libraryFolderDao().insert(LibraryFolderEntity("R", "Books", Instant.EPOCH))
            tree.put("three", bytes = TestEpubBuilder().title("Three").calibreSeries(" Earthsea ", 2.5).build())
            tree.put("one", bytes = TestEpubBuilder().title("One").calibreSeries("earthsea", 1.0).build())
            settings.setSeriesAutoFill(false)
            runner.run()
            assertTrue(organization.get().categories.isEmpty())
            settings.setSeriesAutoFill(true)
            assertEquals(2, runner.run().unchanged)
            val automatic = organization.get().categories.single()
            assertTrue(automatic.needsReview)
            assertEquals(listOf(1.0, 2.5), automatic.books.map { it.membership.position })
            val manual = organization.saveCategory(name = "My series", typeIds = setOf(1L))
            organization.mergeCategories(automatic.category.id, manual)
            tree.put("two", bytes = TestEpubBuilder().title("Two").calibreSeries("EARTHSEA", 2.0).build())
            runner.run()
            val merged = organization.get().categories.single()
            assertEquals(manual, merged.category.id)
            assertEquals(listOf(1.0, 2.0, 2.5), merged.books.map { it.membership.position })
            val hidden =
                merged.books
                    .last()
                    .book.book.id
            db.openHelper.writableDatabase.execSQL("UPDATE books SET ignored = 1 WHERE id = ?", arrayOf(hidden))
            runner.run()
            assertEquals(
                2,
                organization
                    .get()
                    .categories
                    .single()
                    .books.size,
            )
            organization.removeMember(
                manual,
                merged.books
                    .first()
                    .book.book.id,
            )
            runner.run()
            assertEquals(
                listOf(2.0),
                organization
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.membership.position },
            )
        }
}
