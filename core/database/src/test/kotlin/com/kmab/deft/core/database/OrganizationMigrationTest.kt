package com.kmab.deft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrganizationMigrationTest {
    @Test fun organizationSchemaIsInstalledOnFreshDatabase() {
        val db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).build()
        try {
            assertEquals(9, db.openHelper.writableDatabase.version)
        } finally {
            db.close()
        }
    }

    @Test fun versionSixPreservesBookAndSessionDataAndSeedsSeriesWithSafeForeignKeys() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "organization-migration.db"
            context.deleteDatabase(name)
            createVersionSix(context, name)
            val db =
                Room
                    .databaseBuilder(context, AppDatabase::class.java, name)
                    .addMigrations(
                        DatabaseMigrations.FROM_6_TO_7,
                        DatabaseMigrations.FROM_7_TO_8,
                        DatabaseMigrations.FROM_8_TO_9,
                    ).build()
            try {
                val book = db.bookDao().getById(1)!!
                assertEquals("kept-locator", book.currentLocator)
                assertEquals(4.5, book.rating!!, 0.0)
                assertEquals("kept review", book.review)
                assertEquals(1, db.trackingDao().getReadThroughs(1).size)
                val session = db.trackingDao().getSessions(1).single()
                assertEquals(45L, session.activeSeconds)
                assertEquals(30, session.minimumSessionSeconds)
                assertEquals("kept-start", session.startLocator)
                val series = db.organizationDao().types().single()
                assertEquals(1L, series.id)
                assertTrue(series.builtIn)
                val sql = db.openHelper.writableDatabase
                sql.execSQL(
                    "INSERT INTO categories (id, name, autoCreated, orderMode, createdAt) " +
                        "VALUES (1, 'Parent', 0, 'MANUAL', 0)",
                )
                sql.execSQL(
                    "INSERT INTO categories (id, name, parentId, autoCreated, orderMode, createdAt) " +
                        "VALUES (2, 'Child', 1, 0, 'MANUAL', 0)",
                )
                sql.execSQL(
                    "INSERT INTO category_members (categoryId, bookId, position, suggested, addedAt) " +
                        "VALUES (1, 1, 1.0, 0, 0)",
                )
                db.organizationDao().deleteCategory(1)
                assertNull(db.organizationDao().category(2)!!.parentId)
                assertTrue(db.organizationDao().members().isEmpty())
                assertEquals(
                    45L,
                    db
                        .trackingDao()
                        .getSessions(1)
                        .single()
                        .activeSeconds,
                )
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }

    private fun createVersionSix(
        context: Context,
        name: String,
    ) {
        val schema =
            JSONObject(
                File("schemas/com.kmab.deft.core.database.AppDatabase/6.json").readText(),
            ).getJSONObject("database")
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
                "INSERT INTO books (id, contentHash, format, title, sortTitle, drmProtected, ignored, addedAt, " +
                    "currentLocator, totalProgression, rating, favorite, review) " +
                    "VALUES (1, 'hash', 'EPUB', 'Kept', 'kept', 0, 0, 0, 'kept-locator', 0.7, 4.5, 1, 'kept review')",
            )
            old.execSQL(
                "INSERT INTO read_throughs (id, bookId, startedAt, outcome, finishPromptShown) " +
                    "VALUES (1, 1, 0, 'PAUSED', 0)",
            )
            old.execSQL(
                "INSERT INTO reading_sessions (id, bookId, readThroughId, startedAt, endedAt, " +
                    "activeSeconds, startLocator, startProgression, endProgression, pagesTurned, browsing, " +
                    "zoneOffsetSeconds, finalized, minimumSessionSeconds) " +
                    "VALUES ('kept', 1, 1, 0, 45000, 45, 'kept-start', 0, 0.7, 3, 0, 0, 1, 30)",
            )
            old.version = 6
            assertEquals(6, old.version)
            old.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'", null).use {
                assertEquals(0, it.count)
            }
        }
    }
}
