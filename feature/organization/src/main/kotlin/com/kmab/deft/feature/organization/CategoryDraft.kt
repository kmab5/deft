package com.kmab.deft.feature.organization

import android.net.Uri
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.model.CategoryOrder
import com.kmab.deft.library.organization.OrganizationRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class CategoryDraft(
    val name: String,
    val description: String,
    val typeIds: Set<Long>,
    val cover: Uri?,
    val removeCover: Boolean,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && name.length <= MAX_NAME_LENGTH && description.length <= MAX_DESCRIPTION_LENGTH
}

internal suspend fun OrganizationRepository.persistCategory(
    draft: CategoryDraft,
    category: CategoryEntity?,
    initialBooks: Set<Long>?,
    covers: CategoryCoverStore,
) {
    persistCategoryCover(
        previous = category?.coverPath,
        removePrevious = draft.removeCover,
        importCover = { draft.cover?.let { covers.importImage(it) } },
        removeCover = covers::remove,
    ) { path ->
        val description = draft.description.ifBlank { null }
        if (category == null && initialBooks != null) {
            createCategoryWithBooks(draft.name, description, draft.typeIds, coverPath = path, bookIds = initialBooks)
        } else {
            saveCategory(
                category?.id,
                draft.name,
                description,
                draft.typeIds,
                category?.order ?: CategoryOrder.MANUAL,
                path,
            )
        }
    }
}

internal suspend fun persistCategoryCover(
    previous: String?,
    removePrevious: Boolean,
    importCover: suspend () -> String?,
    removeCover: suspend (String?) -> Unit,
    commit: suspend (String?) -> Unit,
) {
    currentCoroutineContext().ensureActive()
    // Keep the staged file and its database reference consistent across dispatcher returns.
    withContext(NonCancellable) {
        val imported = importCover()
        val path = imported ?: previous.takeUnless { removePrevious }
        var persisted = false
        try {
            commit(path)
            persisted = true
        } finally {
            if (!persisted) {
                removeCover(imported)
            }
        }
        if (previous != path) removeCover(previous)
    }
}

private const val MAX_NAME_LENGTH = 240
private const val MAX_DESCRIPTION_LENGTH = 10_000
