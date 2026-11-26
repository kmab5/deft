package com.kmab.deft.feature.reader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderAnnotationPersistenceTest {
    @Test fun explicitSaveSurvivesReopenWhileSelectionCancelAndFailedSaveNeverLoseDraft(): Unit =
        runBlocking {
            val db =
                Room
                    .inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext<Context>(),
                        AppDatabase::class.java,
                    ).build()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val bookId = db.bookDao().insert(book())
                val repository = NotesRepository(db)
                val messages = mutableListOf<String>()
                val controller = ReaderAnnotations(scope, repository, messages::add)
                controller.observe(bookId)
                val locator =
                    parseLocator(
                        """
                        {"href":"chapter.xhtml","type":"application/xhtml+xml",
                        "text":{"highlight":"A passage"}}
                        """.trimIndent(),
                    )!!
                controller.select(bookId, locator, highlight = true)
                assertTrue(repository.observeHighlights(bookId).first().isEmpty())
                controller.dismiss()
                assertNull(controller.state.value.draft)
                controller.select(bookId, locator, highlight = false)
                val draft =
                    controller.state.value.draft!!
                        .copy(note = "Remember")
                controller.save(draft)
                controller.save(draft)
                withTimeout(
                    5_000,
                ) { controller.state.first { !it.busy && it.draft == null && it.highlights.size == 1 } }
                val reopened = NotesRepository(db).observeHighlights(bookId).first().single()
                assertEquals("Remember", reopened.note)
                assertEquals(HighlightColor.NONE, reopened.color)
                assertTrue(readerDecorations(listOf(reopened)).isEmpty())
                controller.edit(reopened.id)
                controller.save(
                    controller.state.value.draft!!
                        .copy(bookId = -1),
                )
                withTimeout(5_000) { controller.state.first { !it.busy } }
                assertNotNull(controller.state.value.draft)
                assertTrue(messages.any { it.contains("could not be saved") })
                assertEquals(1, repository.observeHighlights(bookId).first().size)
                controller.delete()
                withTimeout(
                    5_000,
                ) { controller.state.first { !it.busy && it.draft == null && it.highlights.isEmpty() } }
            } finally {
                scope.cancel()
                db.close()
            }
        }

    private fun book() =
        BookEntity(
            contentHash = "book",
            format = BookFormat.EPUB,
            title = "Book",
            sortTitle = "Book",
            addedAt = Instant.EPOCH,
        )
}
