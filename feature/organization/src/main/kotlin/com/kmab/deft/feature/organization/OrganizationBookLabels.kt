package com.kmab.deft.feature.organization

import com.kmab.deft.core.database.model.BookListItem

internal fun BookListItem.displayTitle(): String = title.trim().ifEmpty { "<no-title>" }

internal fun BookListItem.displayAuthors(): String = authorsDisplay.trim().ifEmpty { "<no-author>" }
