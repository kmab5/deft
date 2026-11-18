package com.kmab.deft.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
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
}
