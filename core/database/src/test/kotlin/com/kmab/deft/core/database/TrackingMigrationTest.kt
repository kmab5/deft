package com.kmab.deft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrackingMigrationTest {
    @Test fun versionFourKeepsBooksLocatorsAndBookmarksWithEmptyTracking() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "tracking-migration-test.db"
            context.deleteDatabase(name)
            createVersionFour(context, name)
            val db =
                Room
                    .databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        name,
                    ).addMigrations(DatabaseMigrations.FROM_4_TO_5)
                    .build()
            try {
                val book = db.bookDao().getById(1)!!
                assertEquals("kept-locator", book.currentLocator)
                assertEquals(0.7, book.totalProgression, 0.0)
                assertTrue(book.ignored)
                assertNull(book.rating)
                assertFalse(book.favorite)
                assertNull(book.review)
                assertEquals(
                    "kept bookmark",
                    db
                        .bookmarkDao()
                        .observeForBook(1)
                        .first()
                        .single()
                        .label,
                )
                assertTrue(db.trackingDao().getReadThroughs(1).isEmpty())
                assertTrue(db.trackingDao().getSessions(1).isEmpty())
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }

    private fun createVersionFour(
        context: Context,
        name: String,
    ) {
        val schema =
            JSONObject(
                File("schemas/com.kmab.deft.core.database.AppDatabase/4.json").readText(),
            ).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (i in 0 until indices.length()) {
                    old.execSQL(
                        indices.getJSONObject(i).getString("createSql").replace("${'$'}{TABLE_NAME}", table),
                    )
                }
            }
            old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table VALUES(42, ?)", arrayOf(schema.getString("identityHash")))
            old.execSQL(
                "INSERT INTO books (id, contentHash, format, title, sortTitle, " +
                    "drmProtected, ignored, addedAt, currentLocator, totalProgression) " +
                    "VALUES (1, 'hash', 'EPUB', 'Kept', 'kept', 0, 1, 0, 'kept-locator', 0.7)",
            )
            old.execSQL(
                "INSERT INTO bookmarks (id, bookId, locator, totalProgression, label, " +
                    "createdAt) VALUES (1, 1, 'bookmark', 0.2, 'kept bookmark', 0)",
            )
            old.version = 4
        }
    }
}
