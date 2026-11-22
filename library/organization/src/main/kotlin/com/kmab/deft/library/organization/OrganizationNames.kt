package com.kmab.deft.library.organization

import java.util.Locale

internal object OrganizationNames {
    const val MAX_NAME_LENGTH = 240
    const val MAX_DESCRIPTION_LENGTH = 10_000
    const val MAX_POSITION = 1_000_000_000.0
    const val SERIES_ID = 1L
    private val whitespace = Regex("[\\s\\p{Z}]+")

    fun display(raw: String): String = raw.replace(whitespace, " ").trim()

    fun key(raw: String): String = display(raw).lowercase(Locale.ROOT)

    fun name(raw: String): String =
        display(raw).also {
            require(it.isNotBlank()) { "Enter a name." }
            require(it.length <= MAX_NAME_LENGTH) { "Names can have at most $MAX_NAME_LENGTH characters." }
        }

    fun description(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.also {
            require(
                it.length <= MAX_DESCRIPTION_LENGTH,
            ) { "Descriptions can have at most $MAX_DESCRIPTION_LENGTH characters." }
        }

    fun position(value: Double): Double =
        value.also {
            require(it.isFinite() && it in -MAX_POSITION..MAX_POSITION) {
                "Enter a finite position between -1 billion and 1 billion."
            }
        }

    fun appendPosition(existing: Collection<Double>): Double =
        ((existing.maxOrNull() ?: 0.0) + 1).coerceAtMost(MAX_POSITION)
}
