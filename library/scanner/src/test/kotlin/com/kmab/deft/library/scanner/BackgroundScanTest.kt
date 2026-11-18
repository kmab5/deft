package com.kmab.deft.library.scanner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackgroundScanTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    @After fun close() = db.close()

    @Test fun removalAndIntentCommitTogetherAndReaddDoesNotLoseIntent() =
        runBlocking {
            val dao = db.libraryFolderDao()
            dao.insert(LibraryFolderEntity("root", "Books", Instant.EPOCH))
            dao.removeForReconciliation("root")
            assertTrue(dao.getAll().isEmpty())
            assertEquals(listOf("root"), dao.pendingRemovals())
            dao.insert(LibraryFolderEntity("root", "Books", Instant.EPOCH))
            assertEquals(listOf("root"), dao.pendingRemovals())
            dao.acknowledgeRemovals(listOf("root"))
            assertTrue(dao.pendingRemovals().isEmpty())
            assertEquals(1, dao.getAll().size)
        }

    @Test fun removalTransactionRollsBackIntentWhenFolderDeleteFails() =
        runBlocking {
            val dao = db.libraryFolderDao()
            dao.insert(LibraryFolderEntity("root", "Books", Instant.EPOCH))
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_removal BEFORE DELETE ON library_folders " +
                    "BEGIN SELECT RAISE(ABORT, 'blocked'); END",
            )
            try {
                dao.removeForReconciliation("root")
                error("Expected transaction rollback")
            } catch (_: android.database.sqlite.SQLiteException) {
            }
            assertEquals(listOf("root"), dao.getAll().map { it.treeUri })
            assertTrue(dao.pendingRemovals().isEmpty())
        }

    @Test fun pendingRemovalSurvivesClosingAndReopeningRoom() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val name = "pending-restart-test.db"
            context.deleteDatabase(name)

            fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            open().let { first ->
                first.libraryFolderDao().insert(LibraryFolderEntity("root", "Books", Instant.EPOCH))
                first.libraryFolderDao().removeForReconciliation("root")
                first.close()
            }
            open().let { reopened ->
                try {
                    assertTrue(reopened.libraryFolderDao().getAll().isEmpty())
                    assertEquals(listOf("root"), reopened.libraryFolderDao().pendingRemovals())
                    assertFalse(reopened.libraryFolderDao().pendingRemovals().isEmpty())
                } finally {
                    reopened.close()
                    context.deleteDatabase(name)
                }
            }
        }
}
