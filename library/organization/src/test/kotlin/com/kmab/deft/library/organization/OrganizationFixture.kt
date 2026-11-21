package com.kmab.deft.library.organization

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.ReadThroughEntity
import com.kmab.deft.core.database.entity.ReadingSessionEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ReadThroughOutcome
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import java.time.Instant

abstract class OrganizationFixture {
    protected lateinit var db: AppDatabase
    protected lateinit var repo: OrganizationRepository
    protected val now: Instant = Instant.ofEpochSecond(1000)

    @Before fun openDatabase() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = OrganizationRepository(db)
    }

    @After fun closeDatabase() = db.close()

    protected suspend fun book(
        title: String,
        series: String? = null,
        index: Double? = null,
        rating: Double? = null,
        ignored: Boolean = false,
    ): Long =
        db.bookDao().insert(
            BookEntity(
                contentHash = title,
                format = BookFormat.EPUB,
                title = title,
                sortTitle = title.lowercase(),
                extractedSeriesName = series,
                extractedSeriesIndex = index,
                rating = rating,
                ignored = ignored,
                addedAt = now,
            ),
        )

    protected suspend fun read(
        bookId: Long,
        outcome: ReadThroughOutcome,
    ): Long = db.trackingDao().insertReadThrough(ReadThroughEntity(bookId = bookId, startedAt = now, outcome = outcome))

    protected suspend fun session(
        bookId: Long,
        readId: Long,
        seconds: Long,
        browsing: Boolean = false,
        minimum: Int = 30,
    ) {
        db.trackingDao().upsertSession(
            ReadingSessionEntity(
                id = "$bookId-$seconds-$browsing",
                bookId = bookId,
                readThroughId = readId,
                startedAt = now,
                endedAt = now.plusSeconds(seconds),
                activeSeconds = seconds,
                startLocator = "kept",
                endLocator = "kept",
                startProgression = 0.0,
                endProgression = 0.2,
                pagesTurned = 3,
                startPosition = 1,
                endPosition = 4,
                browsing = browsing,
                zoneOffsetSeconds = 0,
                finalized = true,
                minimumSessionSeconds = minimum,
            ),
        )
    }

    protected suspend fun invalid(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected invalid organization input to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected domain validation failure; no storage changes may precede it.
        }
    }
}
