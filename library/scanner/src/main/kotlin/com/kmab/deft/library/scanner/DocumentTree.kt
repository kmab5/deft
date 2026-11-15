package com.kmab.deft.library.scanner

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

data class DocumentEntry(
    val uri: String,
    val rootUri: String,
    val displayPath: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
)

/** Read-only provider seam. Walk emits files lazily and throws if enumeration is incomplete.
 * Exclusions are provider document IDs; adapters resolve ancestry, never URI prefixes.
 */
interface DocumentTree {
    suspend fun isAccessible(rootUri: String): Boolean

    /** True for the same provider document or its descendant. Throw when ancestry cannot be resolved. */
    suspend fun isChild(
        parentRootUri: String,
        childRootUri: String,
    ): Boolean

    fun walk(
        rootUri: String,
        excludedDocumentIds: Set<String>,
    ): Flow<DocumentEntry>

    fun open(uri: String): InputStream
}
