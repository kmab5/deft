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
)
