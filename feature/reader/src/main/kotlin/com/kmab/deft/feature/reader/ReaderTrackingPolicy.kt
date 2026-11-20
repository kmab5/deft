package com.kmab.deft.feature.reader

import com.kmab.deft.core.model.BookState

internal fun requiresReadingChoice(state: BookState): Boolean =
    state == BookState.FINISHED || state == BookState.ABANDONED

internal fun reachedReadingEnd(
    progression: Double,
    position: Int?,
    totalPositions: Int,
): Boolean = progression >= END_PROGRESSION || totalPositions > 0 && position != null && position >= totalPositions

private const val END_PROGRESSION = 0.99
