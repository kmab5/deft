package com.kmab.deft.library.scanner

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ExtractedMetadata
import com.kmab.deft.core.model.MetadataException
import com.kmab.deft.core.model.MetadataExtractor
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** One extractor per format. Caller owns the stream; only metadata/read failures fall back. */
class MetadataExtractorRegistry(
    extractors: Set<MetadataExtractor>,
) {
    private val byFormat =
        buildMap {
            for (extractor in extractors) {
                for (format in extractor.formats) {
                    require(!containsKey(format)) { "Multiple metadata extractors for $format" }
                    put(format, extractor)
                }
            }
        }

    fun extract(
        format: BookFormat,
        input: InputStream,
        fileName: String,
    ): Pair<ExtractedMetadata, String?> {
        val fallback = FilenameMetadata.parse(fileName)
        val extractor = byFormat[format] ?: return fallback to null
        return try {
            val metadata =
                extractor.extract(
                    object : FilterInputStream(input) {
                        override fun close() = Unit
                    },
                    fileName,
                )
            merge(metadata, fallback) to null
        } catch (error: MetadataException) {
            fallback to (error.message ?: error.javaClass.simpleName)
        } catch (error: IOException) {
            fallback to (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun merge(
        metadata: ExtractedMetadata,
        fallback: ExtractedMetadata,
    ): ExtractedMetadata {
        val embeddedSeries = metadata.seriesName
        val series = embeddedSeries ?: fallback.seriesName
        val index =
            if (embeddedSeries == null) {
                fallback.seriesIndex
            } else {
                metadata.seriesIndex ?: fallback.seriesIndex.takeIf {
                    normalizeSeries(embeddedSeries) == fallback.seriesName?.let(::normalizeSeries)
                }
            }
        return metadata.copy(
            title = metadata.title ?: fallback.title,
            authors = metadata.authors.ifEmpty { fallback.authors },
            seriesName = series,
            seriesIndex = index,
        )
    }

    private fun normalizeSeries(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)
}
