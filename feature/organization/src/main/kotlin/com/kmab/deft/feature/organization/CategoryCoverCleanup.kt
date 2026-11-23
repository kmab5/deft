package com.kmab.deft.feature.organization

import android.database.sqlite.SQLiteException
import com.kmab.deft.library.organization.OrganizationRepository
import java.io.IOException

internal suspend fun OrganizationRepository.removeUnusedCovers(
    paths: List<String>,
    covers: CategoryCoverStore,
) = removeUnusedCovers(
    paths = paths,
    retainedPaths = { get().categories.mapNotNull { it.category.coverPath }.toSet() },
    removeCover = { covers.remove(it) },
)

// Database success must not be reported as a failed mutation if optional cache cleanup fails.
internal suspend fun removeUnusedCovers(
    paths: List<String>,
    retainedPaths: suspend () -> Set<String>,
    removeCover: suspend (String) -> Unit,
) {
    if (paths.isEmpty()) return
    bestEffortCoverCleanup {
        val retained = retainedPaths()
        paths.distinct().filterNot { it in retained }.forEach { path ->
            bestEffortCoverCleanup { removeCover(path) }
        }
    }
}

private suspend fun bestEffortCoverCleanup(block: suspend () -> Unit) {
    try {
        block()
    } catch (_: SQLiteException) {
        // Failed retention lookup must leave potentially shared covers intact.
    } catch (_: IOException) {
        // An unused private image can remain until app storage is cleared.
    } catch (_: SecurityException) {
        // Keep the successful category operation even if storage is unavailable.
    }
}
