package com.kmab.deft.tracking

import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.ReadThroughEntity
import com.kmab.deft.core.database.entity.ReadingSessionEntity
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.ReadThrough
import com.kmab.deft.core.model.ReadingSession

internal fun BookEntity.assessment() = BookAssessment(rating, favorite, review)

internal fun ReadThroughEntity.model() = ReadThrough(id, bookId, startedAt, endedAt, outcome, finishPromptShown)

internal fun ReadingSessionEntity.model() =
    ReadingSession(
        id,
        bookId,
        readThroughId,
        startedAt,
        endedAt,
        activeSeconds,
        startLocator,
        endLocator,
        startProgression,
        endProgression,
        pagesTurned,
        startPosition,
        endPosition,
        browsing,
        zoneOffsetSeconds,
        finalized,
        minimumSessionSeconds,
    )

internal fun ReadingSession.entity() =
    ReadingSessionEntity(
        id,
        bookId,
        readThroughId,
        startedAt,
        endedAt,
        activeSeconds,
        startLocator,
        endLocator,
        startProgression,
        endProgression,
        pagesTurned,
        startPosition,
        endPosition,
        browsing,
        zoneOffsetSeconds,
        finalized,
        minimumSessionSeconds,
    )
