package com.kmab.deft.library.scanner

/** Added counts new Books; updated counts changed existing file fingerprints. Reattached counts
 * existing Books restored or moved (once per Book). Unchanged counts remaining observed files,
 * including extra copies. Missing counts Books transitioning from present to missing.
 * Errors count failed file reads/metadata/cover operations or incomplete root enumerations.
 */
data class ScanReport(
    val added: Int,
    val updated: Int,
    val unchanged: Int,
    val reattached: Int,
    val markedMissing: Int,
    val errors: Int,
    val unavailableRoots: List<String>,
    val processedFiles: Int = 0,
    val skippedFiles: Int = 0,
    val duplicateFiles: Int = 0,
    val metadataFallbacks: Int = 0,
    val failedFiles: Int = 0,
    val incompleteRoots: List<String> = emptyList(),
    val errorDetails: List<String> = emptyList(),
    val needsContinuation: Boolean = false,
    val unavailableRootCount: Int = unavailableRoots.size,
    val incompleteRootCount: Int = incompleteRoots.size,
) {
    fun summary(): String {
        val outcome =
            if (needsContinuation) {
                "scan continuing"
            } else if (isPartial) {
                "scan incomplete"
            } else {
                "scan complete"
            }
        return "$outcome · $processedFiles files checked · $added books added"
    }

    val isPartial: Boolean get() = unavailableRoots.isNotEmpty() || incompleteRoots.isNotEmpty() || failedFiles > 0

    fun details(): List<String> =
        buildList {
            add("$updated changed · $reattached restored · $markedMissing now missing")
            add("$duplicateFiles duplicate files share an existing book")
            add("$skippedFiles files skipped: disabled or unsupported file types")
            add("$metadataFallbacks files use filename details; their books are still included")
            if (failedFiles > 0) add("$failedFiles files could not be read; try rescan after checking access")
            if (unavailableRoots.isNotEmpty()) {
                val count = maxOf(unavailableRootCount, unavailableRoots.size)
                add("$count folders unavailable; grant access again in library settings")
            }
            if (incompleteRoots.isNotEmpty()) {
                val count = maxOf(incompleteRootCount, incompleteRoots.size)
                add("$count folder listings incomplete; unseen books kept present")
            }
            addAll(errorDetails)
        }
}

data class ScanProgress(
    val processedFiles: Int = 0,
    val skippedFiles: Int = 0,
)
