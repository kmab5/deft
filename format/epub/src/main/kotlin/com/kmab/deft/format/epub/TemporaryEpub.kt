package com.kmab.deft.format.epub

import com.kmab.deft.core.model.MetadataException
import java.io.Closeable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** A private spool whose cleanup participates in use's suppressed-exception handling. */
internal class TemporaryEpub(
    val path: Path,
) : Closeable {
    override fun close() {
        try {
            Files.deleteIfExists(path)
        } catch (error: IOException) {
            throw MetadataException("Cannot remove EPUB temporary spool", error)
        }
    }
}
