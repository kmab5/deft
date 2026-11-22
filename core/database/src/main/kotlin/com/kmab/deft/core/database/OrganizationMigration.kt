package com.kmab.deft.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal object OrganizationMigration {
    fun create(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS category_types (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, normalizedName TEXT NOT NULL, builtIn INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_category_types_normalizedName ON category_types(normalizedName)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, description TEXT, parentId INTEGER, autoCreated INTEGER NOT NULL, " +
                "coverPath TEXT, " +
                "orderMode TEXT NOT NULL, ratingOverride REAL, createdAt INTEGER NOT NULL, " +
                "FOREIGN KEY(parentId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parentId ON categories(parentId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS category_type_links (categoryId INTEGER NOT NULL, typeId INTEGER NOT NULL, " +
                "PRIMARY KEY(categoryId, typeId), " +
                "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(typeId) REFERENCES category_types(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_category_type_links_typeId ON category_type_links(typeId)")
        createMemberships(db)
        db.execSQL(
            "INSERT OR IGNORE INTO category_types (id, name, normalizedName, builtIn) " +
                "VALUES (1, 'Series', 'series', 1)",
        )
    }

    private fun createMemberships(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS category_members (categoryId INTEGER NOT NULL, bookId INTEGER NOT NULL, " +
                "position REAL NOT NULL, suggested INTEGER NOT NULL, addedAt INTEGER NOT NULL, " +
                "PRIMARY KEY(categoryId, bookId), " +
                "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_category_members_bookId ON category_members(bookId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS category_series_aliases (normalizedSeriesName TEXT NOT NULL, " +
                "categoryId INTEGER NOT NULL, " +
                "PRIMARY KEY(normalizedSeriesName), " +
                "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_category_series_aliases_categoryId " +
                "ON category_series_aliases(categoryId)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS category_auto_fill_exclusions (categoryId INTEGER NOT NULL, " +
                "bookId INTEGER NOT NULL, " +
                "PRIMARY KEY(categoryId, bookId), " +
                "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_category_auto_fill_exclusions_bookId " +
                "ON category_auto_fill_exclusions(bookId)",
        )
    }
}
