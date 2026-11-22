package com.kmab.deft.library.organization

import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategorySeriesAliasEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.database.entity.CategoryTypeLinkEntity
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.CategoryOrder
import java.time.Instant

/** Called only inside the repository's transaction. */
internal class CategoryOperations(
    private val dao: OrganizationDao,
) {
    @Suppress("LongParameterList") // Mirrors the single public category edit operation.
    suspend fun save(
        id: Long?,
        name: String,
        description: String?,
        typeIds: Set<Long>,
        order: CategoryOrder,
        coverPath: String?,
    ): Long {
        val cleanName = OrganizationNames.name(name)
        val cleanDescription = OrganizationNames.description(description)
        val types = dao.types().associateBy { it.id }
        require(typeIds.all { it in types }) { "One of these types no longer exists." }
        val old = id?.let { category(it) }
        val series = OrganizationNames.SERIES_ID in typeIds
        val value =
            (old ?: CategoryEntity(name = cleanName, createdAt = Instant.now())).copy(
                name = cleanName,
                description = cleanDescription,
                order = order,
                coverPath = coverPath,
                parentId = null,
                autoCreated = old?.autoCreated == true && series,
            )
        val categoryId = if (old == null) dao.insertCategory(value) else old.id.also { dao.updateCategory(value) }
        if (old?.autoCreated == true && series) preserveOldAlias(old)
        if (!series) dao.clearSeriesAliases(categoryId)
        dao.clearTypes(categoryId)
        dao.insertTypeLinks(typeIds.map { CategoryTypeLinkEntity(categoryId, it) })
        return categoryId
    }

    suspend fun delete(id: Long) {
        category(id)
        dao.deleteCategory(id)
    }

    suspend fun saveType(
        id: Long?,
        name: String,
    ): Long {
        val clean = OrganizationNames.name(name)
        val key = OrganizationNames.key(clean)
        val all = dao.types()
        val old = id?.let { value -> requireNotNull(all.find { it.id == value }) { "This type no longer exists." } }
        require(old?.builtIn != true) { "The built-in Series type cannot be renamed." }
        require(all.none { it.id != id && it.normalizedName == key }) { "A type with this name already exists." }
        val value = CategoryTypeEntity(id = id ?: 0, name = clean, normalizedName = key)
        return if (old == null) dao.insertType(value) else old.id.also { dao.updateType(value) }
    }

    suspend fun deleteType(id: Long) {
        val type = requireNotNull(dao.type(id)) { "This type no longer exists." }
        require(!type.builtIn) { "The built-in Series type cannot be deleted." }
        dao.deleteType(id)
    }

    suspend fun rate(
        id: Long,
        rating: Double?,
    ) {
        BookAssessment(rating = rating)
        dao.updateCategory(category(id).copy(ratingOverride = rating))
    }

    suspend fun category(id: Long): CategoryEntity =
        requireNotNull(dao.category(id)) { "This category no longer exists." }

    private suspend fun preserveOldAlias(old: CategoryEntity) {
        val key = OrganizationNames.key(old.name)
        val owner = dao.seriesAliases().find { it.normalizedSeriesName == key }
        if (owner == null || owner.categoryId == old.id) {
            dao.putSeriesAliases(listOf(CategorySeriesAliasEntity(key, old.id)))
        }
    }
}
