package com.kmab.deft.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    @Suppress("MagicNumber") // Literal schema versions identify this migration.
    val FROM_6_TO_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = OrganizationMigration.create(db)
        }

    val FROM_1_TO_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_root_removals` " +
                        "(`treeUri` TEXT NOT NULL, PRIMARY KEY(`treeUri`))",
                )
            }
        }

    // Literal schema versions keep the upgrade path readable beside its migration.
    @Suppress("MagicNumber")
    val FROM_2_TO_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastReadAt INTEGER")
            }
        }

    @Suppress("MagicNumber") // Literal schema versions identify this migration.
    val FROM_3_TO_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN currentLocator TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN totalProgression REAL NOT NULL DEFAULT 0.0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "bookId INTEGER NOT NULL, locator TEXT NOT NULL, totalProgression REAL NOT NULL, " +
                        "label TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_bookId ON bookmarks(bookId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarks_bookId_locator ON bookmarks(bookId, locator)",
                )
            }
        }

    @Suppress("MagicNumber") // Literal schema versions identify this migration.
    val FROM_4_TO_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN rating REAL")
                db.execSQL("ALTER TABLE books ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE books ADD COLUMN review TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS read_throughs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "bookId INTEGER NOT NULL, startedAt INTEGER NOT NULL, endedAt " +
                        "INTEGER, outcome TEXT NOT NULL, " +
                        "finishPromptShown INTEGER NOT NULL, " +
                        "FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_read_throughs_bookId ON read_throughs(bookId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS reading_sessions (id TEXT NOT NULL, bookId INTEGER NOT NULL, " +
                        "readThroughId INTEGER, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, " +
                        "activeSeconds INTEGER NOT NULL, startLocator TEXT, endLocator TEXT, " +
                        "startProgression REAL NOT NULL, endProgression REAL NOT NULL, pagesTurned INTEGER NOT NULL, " +
                        "startPosition INTEGER, endPosition INTEGER, browsing INTEGER NOT NULL, " +
                        "zoneOffsetSeconds INTEGER NOT NULL, finalized INTEGER NOT NULL, PRIMARY KEY(id), " +
                        "FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(readThroughId) REFERENCES read_throughs(id) ON UPDATE " +
                        "NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_bookId ON reading_sessions(bookId)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reading_sessions_readThroughId ON " +
                        "reading_sessions(readThroughId)",
                )
            }
        }

    @Suppress("MagicNumber") // Literal schema versions identify this migration.
    val FROM_5_TO_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reading_sessions ADD COLUMN minimumSessionSeconds INTEGER NOT NULL DEFAULT 60")
            }
        }
}
