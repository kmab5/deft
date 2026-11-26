package com.kmab.deft.feature.reader

import android.content.Context
import android.content.Intent
import com.kmab.deft.core.datastore.ReaderSettingsRepository
import com.kmab.deft.library.scanner.BookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderLauncher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: ReaderSettingsRepository,
        private val books: BookRepository,
    ) {
        private val completion = MutableStateFlow<Long?>(null)
        val pendingCompletion = completion.asStateFlow()

        fun acknowledgeCompletion() {
            completion.value = null
        }

        internal fun completed(bookId: Long) {
            completion.value = bookId
        }

        /** Returns only an eligible Continue book when the global startup option is enabled. */
        suspend fun startupBook(): Long? =
            if (settings.preferences.first().openLastBook) books.observeContinueReading().first()?.id else null

        fun open(bookId: Long) = launch(bookId, null)

        fun openAt(
            bookId: Long,
            locatorJson: String,
        ) = launch(bookId, locatorJson)

        private fun launch(
            bookId: Long,
            locatorJson: String?,
        ) {
            require(bookId > 0)
            context.startActivity(
                Intent(
                    context,
                    ReaderActivity::class.java,
                ).putExtra(BOOK_ID, bookId).putExtra(LOCATOR, locatorJson).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        internal companion object {
            const val BOOK_ID = "bookId"
            const val LOCATOR = "initialLocator"
        }
    }
