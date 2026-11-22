package com.kmab.deft.library.organization

import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.entity.CategoryTypeLinkEntity

internal class CategoryMerger(
    private val dao: OrganizationDao,
) {
    suspend fun merge(
        sourceId: Long,
        targetId: Long,
    ) {
        require(sourceId != targetId) { "Choose a different category to merge into." }
        requireNotNull(dao.category(sourceId)) { "The source category no longer exists." }
        requireNotNull(dao.category(targetId)) { "The destination category no longer exists." }
        val types = dao.typeLinks().filter { it.categoryId == sourceId || it.categoryId == targetId }
        dao.insertTypeLinks(types.map { CategoryTypeLinkEntity(targetId, it.typeId) }.distinct())
        val target = dao.members(targetId).associateBy { it.bookId }.toMutableMap()
        dao.members(sourceId).forEach { source ->
            target.putIfAbsent(source.bookId, source.copy(categoryId = targetId))
        }
        dao.putMembers(target.values.toList())
        val aliases = dao.seriesAliases().filter { it.categoryId == sourceId }.map { it.copy(categoryId = targetId) }
        dao.putSeriesAliases(aliases)
        val exclusions =
            dao
                .exclusions()
                .filter { it.categoryId == sourceId || it.categoryId == targetId }
                .filter { it.bookId !in target }
                .map { it.copy(categoryId = targetId) }
                .distinct()
        dao.clearExclusions(targetId)
        dao.insertExclusions(exclusions)
        dao.deleteCategory(sourceId)
    }
}
