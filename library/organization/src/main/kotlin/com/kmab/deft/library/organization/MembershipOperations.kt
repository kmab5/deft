package com.kmab.deft.library.organization

import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.entity.CategoryAutoFillExclusionEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.model.CategoryOrder
import java.time.Instant

internal class MembershipOperations(
    private val dao: OrganizationDao,
) {
    suspend fun add(
        categoryId: Long,
        bookIds: Set<Long>,
    ) {
        requireNotNull(dao.category(categoryId)) { "This category no longer exists." }
        val existingBooks = dao.bookIds().toHashSet()
        require(bookIds.all { it in existingBooks }) { "One of these books is no longer in Deft." }
        val members = dao.members(categoryId).associateBy { it.bookId }.toMutableMap()
        var position = OrganizationNames.appendPosition(members.values.map { it.position })
        val addedAt = Instant.now()
        val changed =
            bookIds.sorted().map { bookId ->
                dao.clearExclusion(categoryId, bookId)
                val member =
                    members[bookId]?.copy(suggested = false)
                        ?: CategoryMemberEntity(categoryId, bookId, position, false, addedAt)
                position = (position + 1).coerceAtMost(OrganizationNames.MAX_POSITION)
                member
            }
        dao.putMembers(changed)
    }

    suspend fun remove(
        categoryId: Long,
        bookId: Long,
    ) {
        requireNotNull(dao.category(categoryId)) { "This category no longer exists." }
        require(bookId in dao.bookIds()) { "This book is no longer in Deft." }
        dao.deleteMember(categoryId, bookId)
        dao.insertExclusions(listOf(CategoryAutoFillExclusionEntity(categoryId, bookId)))
    }

    suspend fun position(
        categoryId: Long,
        bookId: Long,
        position: Double,
    ) {
        OrganizationNames.position(position)
        require(dao.typeLinks().any { it.categoryId == categoryId && it.typeId == OrganizationNames.SERIES_ID }) {
            "Decimal positions are available for Series categories."
        }
        val member =
            requireNotNull(
                dao.members(categoryId).find { it.bookId == bookId },
            ) { "This book is no longer in the category." }
        dao.putMembers(listOf(member.copy(position = position, suggested = false)))
    }

    suspend fun reorder(
        categoryId: Long,
        visibleBookIds: List<Long>,
    ) {
        val category = requireNotNull(dao.category(categoryId)) { "This category no longer exists." }
        val series = dao.typeLinks().any { it.categoryId == categoryId && it.typeId == OrganizationNames.SERIES_ID }
        require(!series && category.order == CategoryOrder.MANUAL) { "Choose manual ordering before reordering books." }
        val visible = dao.visibleBookIds().toHashSet()
        val members = dao.members(categoryId).filter { it.bookId in visible }.associateBy { it.bookId }
        require(visibleBookIds.size == members.size && visibleBookIds.toSet() == members.keys) {
            "The category changed. Try reordering its current books again."
        }
        dao.putMembers(
            visibleBookIds.mapIndexed { index, bookId ->
                members.getValue(bookId).copy(position = index + 1.0, suggested = false)
            },
        )
    }

    suspend fun reorderSeries(
        categoryId: Long,
        visibleBookIds: List<Long>,
    ) {
        requireNotNull(dao.category(categoryId)) { "This category no longer exists." }
        require(dao.typeLinks().any { it.categoryId == categoryId && it.typeId == OrganizationNames.SERIES_ID }) {
            "Numbered reordering is available for Series categories."
        }
        val visible = dao.visibleBookIds().toHashSet()
        val members = dao.members(categoryId).filter { it.bookId in visible }.associateBy { it.bookId }
        require(visibleBookIds.size == members.size && visibleBookIds.toSet() == members.keys) {
            "The category changed. Try reordering its current books again."
        }
        val slots = members.values.map { it.position }.sorted()
        val unique = slots.distinct().size == slots.size
        dao.putMembers(
            visibleBookIds.mapIndexed { index, id ->
                members.getValue(id).copy(position = if (unique) slots[index] else index + 1.0, suggested = false)
            },
        )
    }

    suspend fun confirm(
        categoryId: Long,
        bookIds: Set<Long>,
    ) {
        requireNotNull(dao.category(categoryId)) { "This category no longer exists." }
        val members = dao.members(categoryId).associateBy { it.bookId }
        require(bookIds.all { it in members }) { "One of these books is no longer in the category." }
        dao.putMembers(bookIds.map { members.getValue(it).copy(suggested = false) })
    }
}
