package com.kmab.deft.library.scanner

import com.kmab.deft.core.model.ExtractedMetadata

/** Filename fallback, ordered from specific series patterns to author/title. */
object FilenameMetadata {
    private val tags = Regex("\\[(?:retail|epub|pdf|scan|ocr|v\\d+|\\d{4})]", RegexOption.IGNORE_CASE)
    private val numbered = Regex("^(.+?) - (\\d+(?:\\.\\d+)?) - (.+)$")
    private val parenthesized = Regex("^(.+?) \\((.+?) #(\\d+(?:\\.\\d+)?)\\)$")

    fun parse(fileName: String): ExtractedMetadata {
        val stem =
            if (fileName.endsWith(FB2_ZIP, ignoreCase = true)) {
                fileName.dropLast(FB2_ZIP.length)
            } else {
                fileName.substringBeforeLast('.', fileName)
            }
        val clean = tags.replace(stem.replace('_', ' '), "").trim()
        val number = numbered.matchEntire(clean)?.destructured
        val parentheses = parenthesized.matchEntire(clean)?.destructured
        val parts = clean.split(" - ")
        return when {
            number != null -> {
                val (series, index, title) = number
                ExtractedMetadata.EMPTY.copy(
                    title = title.trim(),
                    seriesName = series.trim(),
                    seriesIndex = index.toDouble(),
                )
            }
            parentheses != null -> {
                val (title, series, index) = parentheses
                ExtractedMetadata.EMPTY.copy(
                    title = title.trim(),
                    seriesName = series.trim(),
                    seriesIndex = index.toDouble(),
                )
            }
            parts.size == 2 -> ExtractedMetadata.EMPTY.copy(title = parts[1].trim(), authors = listOf(parts[0].trim()))
            else -> ExtractedMetadata.EMPTY.copy(title = clean)
        }
    }

    private const val FB2_ZIP = ".fb2.zip"
}
