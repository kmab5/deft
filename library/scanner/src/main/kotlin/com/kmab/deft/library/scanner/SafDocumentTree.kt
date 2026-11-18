package com.kmab.deft.library.scanner

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafDocumentTree
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : DocumentTree {
        private val resolver = context.contentResolver

        override suspend fun isAccessible(rootUri: String): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    folderName(Uri.parse(rootUri))
                    true
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            }

        override suspend fun isChild(
            parentRootUri: String,
            childRootUri: String,
        ): Boolean =
            withContext(Dispatchers.IO) {
                val parent = Uri.parse(parentRootUri)
                val child = Uri.parse(childRootUri)
                if (parent.authority != child.authority) return@withContext false
                val parentId = documentId(parent)
                val childId = documentId(child)
                parentId == childId ||
                    providerIsChild(
                        DocumentsContract.buildDocumentUriUsingTree(parent, parentId),
                        DocumentsContract.buildDocumentUriUsingTree(child, childId),
                    )
            }

        suspend fun folderName(uri: Uri): String =
            withContext(Dispatchers.IO) {
                query(DocumentsContract.buildDocumentUriUsingTree(uri, documentId(uri))).use { cursor ->
                    if (!cursor.moveToFirst() || cursor.string(Document.COLUMN_MIME_TYPE) != Document.MIME_TYPE_DIR) {
                        throw IOException("Selected document is not an available folder")
                    }
                    cursor.string(Document.COLUMN_DISPLAY_NAME)
                }
            }

        override fun walk(
            rootUri: String,
            excludedDocumentIds: Set<String>,
        ): Flow<DocumentEntry> =
            flow {
                val root = Uri.parse(rootUri)
                val visited = mutableSetOf<String>()

                suspend fun visit(
                    id: String,
                    path: String,
                ) {
                    currentCoroutineContext().ensureActive()
                    if (!visited.add(id)) throw IOException("Provider returned a directory cycle")
                    query(DocumentsContract.buildChildDocumentsUriUsingTree(root, id)).use { cursor ->
                        while (cursor.moveToNext()) {
                            currentCoroutineContext().ensureActive()
                            val childId = cursor.string(Document.COLUMN_DOCUMENT_ID)
                            val child = DocumentsContract.buildDocumentUriUsingTree(root, childId)
                            // Child-document queries establish each parent edge. Pruning the exact
                            // provider ID skips its whole subtree without enumerating excluded files.
                            if (childId in excludedDocumentIds) continue
                            val name = cursor.string(Document.COLUMN_DISPLAY_NAME)
                            val displayPath = "$path/$name"
                            if (cursor.string(Document.COLUMN_MIME_TYPE) == Document.MIME_TYPE_DIR) {
                                visit(childId, displayPath)
                            } else {
                                emit(
                                    DocumentEntry(
                                        child.toString(),
                                        rootUri,
                                        displayPath,
                                        name,
                                        cursor.number(Document.COLUMN_SIZE, -1),
                                        cursor.number(Document.COLUMN_LAST_MODIFIED, 0),
                                    ),
                                )
                            }
                        }
                        verifyComplete(cursor)
                    }
                }
                visit(documentId(root), folderName(root))
            }.flowOn(Dispatchers.IO)

        override fun open(uri: String): InputStream =
            resolver.openInputStream(Uri.parse(uri)) ?: throw IOException("Cannot open document")

        private suspend fun providerIsChild(
            parent: Uri,
            child: Uri,
        ): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DocumentsContract.isChildDocument(resolver, parent, child)
            } else {
                descendantByTraversal(parent, documentId(child))
            }

        /** API 26–28 have no public isChildDocument helper. Query actual provider edges instead. */
        internal suspend fun descendantByTraversal(
            parent: Uri,
            childId: String,
        ): Boolean {
            val pending = ArrayDeque<String>()
            pending.add(documentId(parent))
            val visited = mutableSetOf<String>()
            var found = documentId(parent) == childId
            while (pending.isNotEmpty() && !found) {
                currentCoroutineContext().ensureActive()
                val id = pending.removeFirst()
                if (visited.add(id)) {
                    found = queryChildrenForTarget(parent, id, childId, pending)
                }
            }
            return found
        }

        private suspend fun queryChildrenForTarget(
            parent: Uri,
            id: String,
            childId: String,
            pending: ArrayDeque<String>,
        ): Boolean =
            query(DocumentsContract.buildChildDocumentsUriUsingTree(parent, id)).use { cursor ->
                var found = false
                while (!found && cursor.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val candidate = cursor.string(Document.COLUMN_DOCUMENT_ID)
                    found = candidate == childId
                    if (cursor.string(Document.COLUMN_MIME_TYPE) == Document.MIME_TYPE_DIR) pending.add(candidate)
                }
                verifyComplete(cursor)
                found
            }

        private fun query(uri: Uri): Cursor =
            resolver.query(uri, PROJECTION, android.os.Bundle.EMPTY, null)?.also(::verifyComplete)
                ?: throw IOException("Provider returned no listing")

        private fun verifyComplete(cursor: Cursor) {
            val extras = cursor.extras
            if (extras.getBoolean(DocumentsContract.EXTRA_LOADING) ||
                extras.containsKey(DocumentsContract.EXTRA_ERROR)
            ) {
                cursor.close()
                throw IOException("Provider listing is incomplete")
            }
        }

        companion object {
            fun documentId(uri: Uri): String =
                if (uri.pathSegments.firstOrNull() == "document" ||
                    uri.pathSegments.getOrNull(2) == "document"
                ) {
                    DocumentsContract.getDocumentId(uri)
                } else {
                    DocumentsContract.getTreeDocumentId(uri)
                }

            private val PROJECTION =
                arrayOf(
                    Document.COLUMN_DOCUMENT_ID,
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_MIME_TYPE,
                    Document.COLUMN_SIZE,
                    Document.COLUMN_LAST_MODIFIED,
                )
        }
    }

private fun Cursor.string(column: String): String =
    getString(getColumnIndexOrThrow(column)) ?: throw IOException("Missing document metadata")

private fun Cursor.number(
    column: String,
    unknown: Long,
): Long = getColumnIndex(column).let { if (it < 0 || isNull(it)) unknown else getLong(it) }
