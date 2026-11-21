package com.kmab.deft.tracking

import androidx.room.withTransaction
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.model.BookAssessment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingRepository
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun update(
            bookId: Long,
            assessment: BookAssessment,
        ) = update(bookId) { assessment }

        suspend fun update(
            bookId: Long,
            transform: (BookAssessment) -> BookAssessment,
        ) = database.withTransaction {
            val book = requireNotNull(database.bookDao().getById(bookId)) { "Book no longer exists." }
            val assessment = transform(book.assessment())
            database.trackingDao().updateAssessment(
                bookId,
                assessment.rating,
                assessment.favorite,
                assessment.review?.takeIf { it.isNotBlank() },
            )
        }
    }
