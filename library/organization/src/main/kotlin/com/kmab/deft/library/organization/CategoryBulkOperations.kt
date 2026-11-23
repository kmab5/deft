package com.kmab.deft.library.organization

import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.entity.CategoryEntity

/** Validates the complete selection before applying rules inside one repository transaction. */
internal class CategoryBulkOperations(
    private val dao: OrganizationDao,
) {
    private val categories = CategoryOperations(dao)

    suspend fun delete(ids: Set<Long>) {
        selected(ids).forEach { categories.delete(it.id) }
    }

    suspend fun updateTypes(
        ids: Set<Long>,
        typeIds: Set<Long>,
        add: Boolean,
    ) {
        val selected = selected(ids)
        require(typeIds.isNotEmpty()) { "Select at least one type." }
        val availableTypes = dao.types().map { it.id }.toSet()
        require(typeIds.all { it in availableTypes }) { "One of these types no longer exists." }
        val links = dao.typeLinks().groupBy { it.categoryId }
        selected.forEach { category ->
            val current = links[category.id].orEmpty().map { it.typeId }.toSet()
            categories.save(
                id = category.id,
                name = category.name,
                description = category.description,
                typeIds = if (add) current + typeIds else current - typeIds,
                order = category.order,
                coverPath = category.coverPath,
            )
        }
    }

    suspend fun merge(
        sourceIds: Set<Long>,
        targetId: Long,
    ) {
        val selected = selected(sourceIds)
        requireNotNull(dao.category(targetId)) { "The destination category no longer exists." }
        val sources = selected.filter { it.id != targetId }
        require(sources.isNotEmpty()) { "Select at least one other category to merge into this destination." }
        val merger = CategoryMerger(dao)
        sources.forEach { merger.merge(it.id, targetId) }
    }

    private suspend fun selected(ids: Set<Long>): List<CategoryEntity> {
        require(ids.isNotEmpty()) { "Select at least one category." }
        return ids.sorted().map { categories.category(it) }
    }
}
