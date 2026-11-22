package com.kmab.deft.library.organization

import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.entity.AuthorAliasEntity
import com.kmab.deft.core.model.AuthorNames

internal class AuthorOperations(
    private val dao: OrganizationDao,
) {
    suspend fun rename(
        id: Long,
        name: String,
    ) {
        val clean = OrganizationNames.name(AuthorNames.display(name))
        val key = AuthorNames.normalizedKey(clean)
        require(key.isNotBlank()) { "Enter an author name containing letters or numbers." }
        val authors = dao.authors()
        val old = requireNotNull(authors.find { it.id == id }) { "This author no longer exists." }
        val aliases = dao.authorAliases()
        require(
            authors.none { it.id != id && it.normalizedName == key } &&
                aliases.none { it.authorId != id && it.aliasNormalizedName == key },
        ) {
            "This name belongs to another author. Merge the authors instead."
        }
        dao.updateAuthor(old.copy(name = clean, normalizedName = key, sortName = AuthorNames.sortName(clean)))
        dao.putAuthorAliases(listOf(AuthorAliasEntity(old.normalizedName, id)))
    }

    suspend fun merge(
        sourceId: Long,
        targetId: Long,
    ) {
        require(sourceId != targetId) { "Choose a different author to merge into." }
        val authors = dao.authors().associateBy { it.id }
        val source = requireNotNull(authors[sourceId]) { "The source author no longer exists." }
        requireNotNull(authors[targetId]) { "The destination author no longer exists." }
        val links = dao.authorLinks().filter { it.authorId == sourceId || it.authorId == targetId }
        val merged =
            links.groupBy { it.bookId }.values.map { candidates ->
                val target = candidates.find { it.authorId == targetId }
                val override = candidates.find { it.overridden }
                val winner = if (target?.overridden == true) target else override ?: target ?: candidates.first()
                winner.copy(authorId = targetId)
            }
        dao.putAuthorLinks(merged)
        val aliases = dao.authorAliases().filter { it.authorId == sourceId }.map { it.copy(authorId = targetId) }
        dao.putAuthorAliases(aliases + AuthorAliasEntity(source.normalizedName, targetId))
        dao.deleteAuthor(sourceId)
    }
}
