package com.kmab.deft.library.organization

import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.database.entity.CategorySeriesAliasEntity
import com.kmab.deft.core.database.entity.CategoryTypeLinkEntity
import com.kmab.deft.core.database.model.OrganizationBookRow
import java.time.Instant

internal class SeriesAutoFill(
    private val dao: OrganizationDao,
) {
    suspend fun fill() {
        val (automatic, aliases) = targets()
        val excluded = dao.exclusions().map { it.categoryId to it.bookId }.toHashSet()
        val members =
            dao
                .members()
                .groupBy { it.categoryId }
                .mapValues { (_, values) ->
                    values.associateBy { it.bookId }.toMutableMap()
                }.toMutableMap()
        val nextPositions =
            members
                .mapValues { (_, values) ->
                    OrganizationNames.appendPosition(values.values.map { it.position })
                }.toMutableMap()
        val bookOrder =
            compareBy<OrganizationBookRow> {
                validIndex(
                    it.extractedSeriesIndex,
                ) == null
            }.thenBy { it.book.id }
        val books = dao.books().filter { !it.extractedSeriesName.isNullOrBlank() }.sortedWith(bookOrder)
        val now = Instant.now()
        val additions = mutableListOf<CategoryMemberEntity>()
        books.forEach { row ->
            val rawName = requireNotNull(row.extractedSeriesName)
            val key = OrganizationNames.key(rawName)
            val categoryId = aliases[key] ?: automatic[key] ?: create(rawName, key, now).also { automatic[key] = it }
            if (key !in aliases) {
                dao.putSeriesAliases(listOf(CategorySeriesAliasEntity(key, categoryId)))
                aliases[key] = categoryId
            }
            val current = members.getOrPut(categoryId) { mutableMapOf() }
            if (row.book.id !in current && (categoryId to row.book.id) !in excluded) {
                val position = validIndex(row.extractedSeriesIndex) ?: nextPositions.getOrDefault(categoryId, 1.0)
                val member = CategoryMemberEntity(categoryId, row.book.id, position, true, now)
                current[row.book.id] = member
                nextPositions[categoryId] =
                    maxOf(nextPositions[categoryId] ?: (position + 1), position + 1)
                        .coerceAtMost(OrganizationNames.MAX_POSITION)
                additions += member
            }
        }
        dao.putMembers(additions)
    }

    private suspend fun targets(): SeriesTargets {
        val series =
            dao
                .typeLinks()
                .filter { it.typeId == OrganizationNames.SERIES_ID }
                .map { it.categoryId }
                .toHashSet()
        val automatic =
            dao
                .categories()
                .filter { it.autoCreated && it.id in series }
                .sortedBy { it.id }
                .groupBy { OrganizationNames.key(it.name) }
                .mapValues { it.value.first().id }
                .toMutableMap()
        val aliases =
            dao
                .seriesAliases()
                .filter { it.categoryId in series }
                .associate {
                    it.normalizedSeriesName to
                        it.categoryId
                }.toMutableMap()
        return SeriesTargets(automatic, aliases)
    }

    private suspend fun create(
        name: String,
        key: String,
        now: Instant,
    ): Long {
        val display = OrganizationNames.display(name).take(OrganizationNames.MAX_NAME_LENGTH).trimEnd()
        val id = dao.insertCategory(CategoryEntity(name = display, autoCreated = true, createdAt = now))
        dao.insertTypeLinks(listOf(CategoryTypeLinkEntity(id, OrganizationNames.SERIES_ID)))
        dao.putSeriesAliases(listOf(CategorySeriesAliasEntity(key, id)))
        return id
    }

    private fun validIndex(value: Double?): Double? =
        value?.takeIf { it.isFinite() && it in -OrganizationNames.MAX_POSITION..OrganizationNames.MAX_POSITION }
}

private data class SeriesTargets(
    val automatic: MutableMap<String, Long>,
    val aliases: MutableMap<String, Long>,
)
