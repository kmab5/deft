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
}
