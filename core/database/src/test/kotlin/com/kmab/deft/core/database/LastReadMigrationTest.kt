package com.kmab.deft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LastReadMigrationTest {
    @Test fun versionTwoPreservesBooksAndDoesNotInventReadingHistory() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "last-read-migration-test.db"
            context.deleteDatabase(name)
            createVersionTwoFixture(context, name)
            val migrated =
                Room
                    .databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        name,
                    ).addMigrations(
                        DatabaseMigrations.FROM_1_TO_2,
                        DatabaseMigrations.FROM_2_TO_3,
                        DatabaseMigrations.FROM_3_TO_4,
                        DatabaseMigrations.FROM_4_TO_5,
                        DatabaseMigrations.FROM_5_TO_6,
                    ).build()
            try {
                assertEquals(
                    listOf(LibraryFolderEntity("root", "Books", Instant.EPOCH)),
                    migrated.libraryFolderDao().getAll(),
                )
                val book = requireNotNull(migrated.bookDao().findByHash("hash"))
                assertEquals("Kept", book.title)
                assertEquals("My title", book.titleOverride)
                assertTrue(book.ignored)
                assertEquals(Instant.ofEpochMilli(1234), book.lastOpenedAt)
                org.junit.Assert.assertNull(book.lastReadAt)
                assertEquals("content://book", migrated.bookFileDao().findByUri("content://book")?.uri)
                assertTrue(migrated.libraryFolderDao().pendingRemovals().isEmpty())
                migrated.libraryFolderDao().removeForReconciliation("root")
                assertEquals(listOf("root"), migrated.libraryFolderDao().pendingRemovals())
            } finally {
                migrated.close()
                context.deleteDatabase(name)
            }
        }

    private fun createVersionTwoFixture(
        context: Context,
        name: String,
    ) {
        val schema =
            JSONObject(
                File("schemas/com.kmab.deft.core.database.AppDatabase/2.json").readText(),
            ).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(
                    entity.getString("createSql").replace("${'$'}{TABLE_NAME}", entity.getString("tableName")),
                )
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) {
                    old.execSQL(
                        indices
                            .getJSONObject(
                                j,
                            ).getString("createSql")
                            .replace("${'$'}{TABLE_NAME}", entity.getString("tableName")),
                    )
                }
            }
            old.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table VALUES(42, ?)", arrayOf(schema.getString("identityHash")))
            old.execSQL("INSERT INTO library_folders VALUES ('root', 'Books', 0)")
            old.execSQL(
                "INSERT INTO books (contentHash, format, title, sortTitle, drmProtected, ignored, addedAt) " +
                    "VALUES ('hash', 'EPUB', 'Kept', 'kept', 0, 0, 0)",
            )
            old.execSQL(
                "UPDATE books SET lastOpenedAt = 1234, titleOverride = 'My title', ignored = 1 " +
                    "WHERE contentHash = 'hash'",
            )
            old.execSQL(
                "INSERT INTO book_files (bookId, rootUri, uri, displayPath, size, " +
                    "lastModified, missing, lastSeenAt) " +
                    "VALUES (1, 'root', 'content://book', 'Kept.epub', 100, 0, 0, 0)",
            )
            old.version = 2
        }
    }
}
