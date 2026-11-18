package com.kmab.deft.library.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface FolderAccess {
    suspend fun displayName(uri: Uri): String

    fun hasRead(uri: Uri): Boolean

    fun takeRead(uri: Uri)

    fun releaseRead(uri: Uri)
}

class SafFolderAccess
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val tree: SafDocumentTree,
    ) : FolderAccess {
        private val resolver = context.contentResolver

        override suspend fun displayName(uri: Uri): String = tree.folderName(uri)

        override fun hasRead(uri: Uri): Boolean =
            resolver.persistedUriPermissions.any {
                it.uri == uri &&
                    it.isReadPermission
            }

        override fun takeRead(uri: Uri) =
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        override fun releaseRead(uri: Uri) {
            if (hasRead(uri)) resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
