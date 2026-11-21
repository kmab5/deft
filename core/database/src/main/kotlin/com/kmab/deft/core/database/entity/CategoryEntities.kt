package com.kmab.deft.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmab.deft.core.model.CategoryOrder
import java.time.Instant

@Entity(tableName = "category_types", indices = [Index(value = ["normalizedName"], unique = true)])
data class CategoryTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val builtIn: Boolean = false,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("parentId")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val parentId: Long? = null,
    val autoCreated: Boolean = false,
    val coverPath: String? = null,
    @ColumnInfo(name = "orderMode") val order: CategoryOrder = CategoryOrder.MANUAL,
    val ratingOverride: Double? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "category_type_links",
    primaryKeys = ["categoryId", "typeId"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["typeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("typeId")],
)
data class CategoryTypeLinkEntity(
    val categoryId: Long,
    val typeId: Long,
)

@Entity(
    tableName = "category_members",
    primaryKeys = ["categoryId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class CategoryMemberEntity(
    val categoryId: Long,
    val bookId: Long,
    val position: Double,
    val suggested: Boolean,
    val addedAt: Instant,
)

@Entity(
    tableName = "category_series_aliases",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class CategorySeriesAliasEntity(
    @PrimaryKey val normalizedSeriesName: String,
    val categoryId: Long,
)

@Entity(
    tableName = "category_auto_fill_exclusions",
    primaryKeys = ["categoryId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class CategoryAutoFillExclusionEntity(
    val categoryId: Long,
    val bookId: Long,
)
