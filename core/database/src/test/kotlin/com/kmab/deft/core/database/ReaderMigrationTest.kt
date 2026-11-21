package com.kmab.deft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderMigrationTest {
    @Test fun versionThreePreservesHistoryAndCreatesDurableBookmarkStorage() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "reader-migration-test.db"
            context.deleteDatabase(name)
            createVersionThree(context, name)
            val migrated = open(context, name)
            try {
                val book = requireNotNull(migrated.bookDao().getById(1))
                assertEquals("My title", book.titleOverride)
                assertTrue(book.ignored)
                assertEquals(Instant.ofEpochMilli(1234), book.lastReadAt)
                assertEquals(Instant.ofEpochMilli(1200), book.lastOpenedAt)
                assertNull(book.currentLocator)
                assertEquals(0.0, book.totalProgression, 0.0)
                migrated.bookDao().savePosition(1, """{"href":"one.xhtml"}""", 0.4, Instant.ofEpochMilli(1300))
                migrated.bookmarkDao().insert(
                    BookmarkEntity(
                        bookId = 1,
                        locator = """{"href":"one.xhtml"}""",
                        totalProgression = 0.4,
                        label = "Kept",
                        createdAt = Instant.EPOCH,
                    ),
                )
            } finally {
                migrated.close()
            }
            val reopened = open(context, name)
            try {
                assertEquals(0.4, reopened.bookDao().getById(1)!!.totalProgression, 0.0)
                assertEquals(
                    "Kept",
                    reopened
                        .bookmarkDao()
                        .observeForBook(1)
                        .first()
                        .single()
                        .label,
                )
                reopened.bookDao().forget(1)
                assertTrue(
                    reopened
                        .bookmarkDao()
                        .observeForBook(1)
                        .first()
                        .isEmpty(),
                )
            } finally {
                reopened.close()
                context.deleteDatabase(name)
            }
        }

    private fun open(
        context: Context,
        name: String,
    ) = Room
        .databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(
            DatabaseMigrations.FROM_1_TO_2,
            DatabaseMigrations.FROM_2_TO_3,
            DatabaseMigrations.FROM_3_TO_4,
            DatabaseMigrations.FROM_4_TO_5,
            DatabaseMigrations.FROM_5_TO_6,
            DatabaseMigrations.FROM_6_TO_7,
        ).build()

    private fun createVersionThree(
        context: Context,
        name: String,
    ) {
        val schema =
            JSONObject(File("schemas/com.kmab.deft.core.database.AppDatabase/3.json").readText())
                .getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (i in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(i).getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                }
            }
            old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table VALUES(42, ?)", arrayOf(schema.getString("identityHash")))
            old.execSQL(
                "INSERT INTO books (id, contentHash, format, title, sortTitle, titleOverride, " +
                    "drmProtected, ignored, addedAt, lastOpenedAt, lastReadAt) " +
                    "VALUES (1, 'hash', 'EPUB', 'Kept', 'kept', 'My title', 0, 1, 0, 1200, 1234)",
            )
            old.version = 3
        }
    }
}
