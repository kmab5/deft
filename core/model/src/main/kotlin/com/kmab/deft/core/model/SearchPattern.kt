package com.kmab.deft.core.model

import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException

data class SearchOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false,
)

/** Linear-time matching with bounded patterns, input and output; never Java's backtracking regex. */
class SearchPattern private constructor(
    private val pattern: Pattern,
    private val wholeWord: Boolean,
) {
    fun contains(text: String): Boolean = ranges(text, 1).isNotEmpty()

    fun ranges(
        text: String,
        limit: Int = 500,
    ): List<IntRange> {
        val input = text.take(MAX_TEXT_LENGTH)
        val matcher = pattern.matcher(input)
        val matches = mutableListOf<IntRange>()
        while (matches.size < limit && matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val boundaryMatches = !wholeWord || (!isWordBefore(input, start) && !isWordAfter(input, end))
            if (start != end && boundaryMatches) matches += start until end
        }
        return matches
    }

    companion object {
        const val MAX_TEXT_LENGTH = 2_000_000
        const val MAX_PATTERN_LENGTH = 256

        fun compile(
            query: String,
            options: SearchOptions = SearchOptions(),
        ): SearchPattern {
            require(
                query.length <= MAX_PATTERN_LENGTH,
            ) { "Use a search pattern of at most $MAX_PATTERN_LENGTH characters." }
            val expression = if (options.regex) query else Pattern.quote(query)
            val pattern =
                try {
                    Pattern.compile(expression, if (options.matchCase) 0 else Pattern.CASE_INSENSITIVE)
                } catch (_: PatternSyntaxException) {
                    throw IllegalArgumentException(
                        "Invalid search pattern. Check brackets and escapes. " +
                            "Lookaround and backreferences are not supported.",
                    )
                }
            return SearchPattern(pattern, options.wholeWord)
        }

        private fun isWord(codePoint: Int): Boolean =
            Character.isLetterOrDigit(codePoint) || codePoint == '_'.code ||
                Character.getType(codePoint) in
                setOf(Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt())

        private fun isWordBefore(
            text: String,
            index: Int,
        ): Boolean = index > 0 && isWord(text.codePointBefore(index))

        private fun isWordAfter(
            text: String,
            index: Int,
        ): Boolean = index < text.length && isWord(text.codePointAt(index))
    }
}
