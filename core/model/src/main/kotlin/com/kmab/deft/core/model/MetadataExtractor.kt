package com.kmab.deft.core.model

import java.io.InputStream

/** Reads metadata without modifying the book. The caller owns and closes [input]. */
interface MetadataExtractor {
    val formats: Set<BookFormat>

    @Throws(MetadataException::class)
    fun extract(
        input: InputStream,
        fileName: String,
    ): ExtractedMetadata
}

class MetadataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
