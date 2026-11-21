package com.kmab.deft.feature.book

import com.kmab.deft.core.model.BookState
import java.time.Instant

/** Commands are kept independent of file availability: missing and protected books retain their history. */
data class BookTrackingActions(
    val setState: (BookState, Instant, Boolean) -> Unit = { _, _, _ -> },
    val editDates: (Long, Instant, Instant?) -> Unit = { _, _, _ -> },
    val delete: (Long) -> Unit = {},
    val rating: (Double?) -> Unit = {},
    val favorite: (Boolean) -> Unit = {},
    val review: (String) -> Unit = {},
)
