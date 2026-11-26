package com.kmab.deft.feature.reader

import android.content.Context
import android.net.Uri
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.feature.readersettings.ReaderFontRepository
import com.kmab.deft.format.readium.OfflineEpubEngine
import com.kmab.deft.format.readium.OpenedEpub
import com.kmab.deft.format.readium.ReaderOpenException
import com.kmab.deft.library.scanner.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.services.locate
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.publication.services.positions
import java.io.IOException

internal class ReaderSessionLoader(
    private val context: Context,
    private val books: BookRepository,
    private val fonts: ReaderFontRepository,
) {
    suspend fun load(
        bookId: Long,
        restored: String?,
    ): ReaderSession {
        val details = books.getBook(bookId) ?: throw ReaderOpenException("This book is no longer in Deft.")
        unreadableReason(details)?.let { throw ReaderOpenException(it) }
        val catalog = fonts.awaitReady()
        val opened = openPresentCopy(details, catalog)
        var retained = false
        try {
            val publication = opened.publication
            val positions = withContext(Dispatchers.Default) { publication.positions() }
            val saved =
                sequenceOf(restored, details.book.currentLocator).mapNotNull(::parseLocator).firstOrNull { point ->
                    publication.readingOrder.any {
                        it.href.toString().substringBefore('#') ==
                            point.href.toString().substringBefore('#')
                    }
                }
            val initial =
                saved?.let { point ->
                    publication.locate(point)?.let { resolved ->
                        point.copy(
                            locations =
                                point.locations.copy(
                                    totalProgression =
                                        point.locations.totalProgression ?: resolved.locations.totalProgression,
                                    position = point.locations.position ?: resolved.locations.position,
                                ),
                        )
                    }
                }
                    ?: publication.locateProgression(details.book.totalProgression.coerceIn(0.0, 1.0))
            val session =
                ReaderSession(
                    bookId,
                    details.book.titleOverride ?: details.book.title,
                    opened,
                    catalog,
                    positions,
                    initial,
                )
            retained = true
            return session
        } finally {
            if (!retained) opened.close()
        }
    }

    private suspend fun openPresentCopy(
        details: BookDetails,
        catalog: List<ReaderFont>,
    ): OpenedEpub {
        var failure: Exception? = null
        for (file in details.files.filterNot { it.missing }) {
            try {
                return OfflineEpubEngine(context).open(Uri.parse(file.uri), details.book.contentHash, catalog)
            } catch (error: IOException) {
                failure = error
            } catch (error: SecurityException) {
                failure = error
            }
        }
        throw failure ?: ReaderOpenException("This book could not be opened.")
    }

    private fun unreadableReason(details: BookDetails): String? =
        when {
            details.book.ignored -> "This book is ignored. Restore it in Library settings before reading."
            details.book.drmProtected -> "Protected books cannot be opened in Deft."
            details.book.format != BookFormat.EPUB ->
                "Deft currently opens EPUB books. This format is not supported yet."
            details.files.none { !it.missing } -> "This book is missing. Reconnect its library folder and scan again."
            else -> null
        }
}
