package com.kmab.deft.library.scanner

import java.util.UUID

enum class ScanDetailOutcome {
    ADDED,
    UPDATED,
    RESTORED,
    UNCHANGED,
    DUPLICATE,
    SKIPPED,
    FALLBACK,
    ERROR,
}

data class ScanDetailEntry(
    val id: String,
    val name: String,
    val path: String,
    val outcome: ScanDetailOutcome,
    val reason: String,
)

data class ScanDetails(
    val entries: List<ScanDetailEntry> = emptyList(),
    /** Omitted diagnostic records, rather than a count of distinct books. */
    val omittedCount: Int = 0,
)

/** Keeps useful import outcomes when a continuation revisits an already indexed file. */
internal class ScanDetailBuffer {
    private val entries = linkedMapOf<String, ScanDetailEntry>()
    private var omitted = 0

    fun add(details: ScanDetails) {
        omitted += details.omittedCount
        details.entries.forEach(::add)
    }

    fun add(entry: ScanDetailEntry) {
        val key = entry.id + if (entry.outcome in DIAGNOSTIC_OUTCOMES) entry.outcome.name else "file"
        val previous = entries[key]
        if (previous != null && entry.outcome == ScanDetailOutcome.UNCHANGED) return
        if (previous != null || entries.size < MAX_ENTRIES) {
            entries[key] =
                entry.copy(
                    id = entry.id.take(MAX_ID_CHARS),
                    name = entry.name.take(MAX_NAME_CHARS),
                    path = entry.path.take(MAX_PATH_CHARS),
                    reason = entry.reason.take(MAX_REASON_CHARS),
                )
        } else {
            omitted++
        }
    }

    fun snapshot(): ScanDetails = ScanDetails(entries.values.toList(), omitted)

    private companion object {
        const val MAX_ENTRIES = 10000
        const val MAX_ID_CHARS = 64
        const val MAX_NAME_CHARS = 160
        const val MAX_PATH_CHARS = 240
        const val MAX_REASON_CHARS = 200
        val DIAGNOSTIC_OUTCOMES = setOf(ScanDetailOutcome.ERROR, ScanDetailOutcome.FALLBACK)
    }
}

internal class ScanDetailRecorder {
    private val records = ScanDetailBuffer()
    private var pending = ScanDetailBuffer()

    fun file(
        entry: DocumentEntry,
        outcome: ScanDetailOutcome,
        reason: String,
    ) {
        add(entry.uri, entry.name, entry.displayPath, outcome, reason)
    }

    fun indexed(
        entry: DocumentEntry,
        changed: Boolean,
        newBook: Boolean,
    ) {
        val outcome =
            when {
                changed -> ScanDetailOutcome.UPDATED
                newBook -> ScanDetailOutcome.ADDED
                else -> ScanDetailOutcome.DUPLICATE
            }
        val reason =
            when (outcome) {
                ScanDetailOutcome.ADDED -> "New book added to your library"
                ScanDetailOutcome.UPDATED -> "Changed file indexed"
                else -> "Same content as an existing book"
            }
        file(entry, outcome, reason)
    }

    fun folder(
        root: String,
        name: String,
        reason: String,
    ) {
        add(root, name, name, ScanDetailOutcome.ERROR, reason)
    }

    private fun add(
        identity: String,
        name: String,
        path: String,
        outcome: ScanDetailOutcome,
        reason: String,
    ) {
        val displayName = name.takeUnless { it.startsWith("content://") } ?: "Book file"
        val displayPath = path.takeUnless { it.startsWith("content://") } ?: displayName
        val entry =
            ScanDetailEntry(
                UUID.nameUUIDFromBytes(identity.toByteArray(Charsets.UTF_8)).toString(),
                displayName,
                displayPath,
                outcome,
                reason,
            )
        records.add(entry)
        pending.add(entry)
    }

    fun drain(): ScanDetails = pending.snapshot().also { pending = ScanDetailBuffer() }

    fun snapshot(): ScanDetails = records.snapshot()
}
