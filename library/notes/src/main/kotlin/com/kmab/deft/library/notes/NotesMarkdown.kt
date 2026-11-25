package com.kmab.deft.library.notes

import kotlin.math.roundToInt

private const val PERCENT_SCALE = 100
private val orderedListPrefix = Regex("^(\\s*\\d+)\\.(?=\\s)")

internal fun notesMarkdown(
    title: String,
    authors: String,
    entries: List<NoteEntry>,
): String =
    buildString {
        appendLine("# ${title.toMarkdownHeading()}")
        if (authors.isNotBlank()) {
            appendLine()
            appendLine(authors.toMarkdownHeading())
        }
        val ordered =
            entries.sortedWith(
                compareBy(
                    NoteEntry::totalProgression,
                    NoteEntry::createdAt,
                    { it.type == NoteType.BOOKMARK },
                    NoteEntry::id,
                ),
            )
        for (entry in ordered) {
            val location = (entry.totalProgression * PERCENT_SCALE).roundToInt()
            val name =
                when (entry.type) {
                    NoteType.HIGHLIGHT -> "Highlight"
                    NoteType.NOTE -> "Note"
                    NoteType.BOOKMARK -> "Bookmark"
                }
            appendLine()
            appendLine("## $name · $location%")
            appendLine()
            if (entry.type == NoteType.BOOKMARK) {
                appendLine(entry.label.orEmpty().toMarkdownHeading())
            } else {
                for (line in entry.text.markdownLines()) {
                    appendLine(if (line.isEmpty()) ">" else "> $line")
                }
                entry.note?.let {
                    appendLine()
                    appendLine(it.markdownLines().joinToString("\n"))
                }
            }
        }
    }

private fun String.toMarkdownHeading(): String = markdownLines().joinToString(" ").trim()

private fun String.markdownLines(): List<String> = lineSequence().map(String::escapeMarkdown).toList()

private fun String.escapeMarkdown(): String =
    buildString {
        for (character in this@escapeMarkdown) {
            if (character in "\\`*_{}[]()<>#+!-|~=&") append('\\')
            append(character)
        }
    }.replace(orderedListPrefix) { "${it.groupValues[1]}\\." }
