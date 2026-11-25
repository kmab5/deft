package com.kmab.deft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HighlightsMigrationTest {
    @Test fun versionSevenPreservesHistoryAndOrganizationAndCreatesHighlights() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "highlights-migration-test.db"
        context.deleteDatabase(name)
        try {
            createVersionSeven(context, name)
            val database =
                Room
                    .databaseBuilder(context, AppDatabase::class.java, name)
                    .addMigrations(DatabaseMigrations.FROM_7_TO_8)
                    .build()
            try {
                val sqlite = database.openHelper.writableDatabase

                fun count(sql: String): Int =
                    sqlite.query(sql).use { cursor ->
                        check(cursor.moveToFirst())
                        cursor.getInt(0)
                    }
                assertEquals(1, count("SELECT count(*) FROM books WHERE titleOverride = 'Edited'"))
                assertEquals(1, count("SELECT count(*) FROM reading_sessions WHERE id = 'session'"))
                assertEquals(1, count("SELECT count(*) FROM bookmarks WHERE label = 'Saved'"))
                assertEquals(1, count("SELECT count(*) FROM category_members WHERE bookId = 1"))
                val highlightsTable = "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'highlights'"
                assertEquals(1, count(highlightsTable))
                assertEquals(0, count("SELECT count(*) FROM pragma_foreign_key_check"))
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun createVersionSeven(
        context: Context,
        name: String,
    ) {
        val schema =
            JSONObject(
                File("schemas/com.kmab.deft.core.database.AppDatabase/7.json").readText(),
            ).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.setForeignKeyConstraintsEnabled(true)
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
                    "drmProtected, ignored, addedAt) " +
                    "VALUES (1, 'hash', 'EPUB', 'Kept', 'kept', 'Edited', 0, 0, 0)",
            )
            old.execSQL(
                "INSERT INTO reading_sessions (id, bookId, startedAt, endedAt, activeSeconds, startProgression, " +
                    "endProgression, pagesTurned, browsing, zoneOffsetSeconds, finalized) " +
                    "VALUES ('session', 1, 0, 60000, 60, 0.0, 0.2, 2, 0, 0, 1)",
            )
            old.execSQL("INSERT INTO bookmarks VALUES (1, 1, 'locator', 0.2, 'Saved', 0)")
            old.execSQL(
                "INSERT INTO categories (id, name, autoCreated, orderMode, createdAt) " +
                    "VALUES (1, 'Shelf', 0, 'MANUAL', 0)",
            )
            old.execSQL("INSERT INTO category_members VALUES (1, 1, 1.0, 0, 0)")
            old.version = 7
        }
    }
}
