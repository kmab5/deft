package com.kmab.deft.feature.reader

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderLauncher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun open(bookId: Long) {
            require(bookId > 0)
            context.startActivity(
                Intent(
                    context,
                    ReaderActivity::class.java,
                ).putExtra(BOOK_ID, bookId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        internal companion object {
            const val BOOK_ID = "bookId"
        }
    }
