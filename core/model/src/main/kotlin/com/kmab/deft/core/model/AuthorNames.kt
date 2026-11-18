package com.kmab.deft.core.model

import java.text.Normalizer
import java.util.Locale

object AuthorNames {
    private val whitespace = Regex("[\\s\\p{Z}]+")
    private val combiningMarks = Regex("\\p{M}+")
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")

    fun display(raw: String): String {
        val collapsed = raw.replace(whitespace, " ").trim()
        if (collapsed.count { it == ',' } != 1) return collapsed
        val (family, given) = collapsed.split(',', limit = 2)
        return "${given.trim()} ${family.trim()}".trim()
    }

    fun normalizedKey(raw: String): String =
        Normalizer
            .normalize(display(raw), Normalizer.Form.NFKD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(nonAlphanumeric, " ")
            .trim()

    fun sortName(display: String): String {
        val name = display.replace(whitespace, " ").trim()
        val lastSpace = name.lastIndexOf(' ')
        return if (lastSpace < 0) name else "${name.substring(lastSpace + 1)}, ${name.substring(0, lastSpace)}"
    }
}
