package com.kmab.deft.library.scanner

import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafDocumentTreeTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val provider =
        TreeProvider().also {
            it.attachInfo(
                context,
                ProviderInfo().apply {
                    authority = "deft.test.documents"
                    exported = true
                    grantUriPermissions = true
                    readPermission = "android.permission.MANAGE_DOCUMENTS"
                    writePermission = "android.permission.MANAGE_DOCUMENTS"
                },
            )
            ShadowContentResolver.registerProviderInternal("deft.test.documents", it)
        }
    private val tree = SafDocumentTree(context)

    private fun root(id: String) = DocumentsContract.buildTreeDocumentUri("deft.test.documents", id).toString()

    @Test fun relationshipsUseProviderIdsAndBothOrders() =
        runBlocking {
            assertTrue(tree.isChild(root("root"), root("unrelated-looking-id")))
            assertFalse(tree.isChild(root("unrelated-looking-id"), root("root")))
            assertTrue(tree.isChild(root("root"), root("root")))
            assertFalse(tree.isChild(root("root"), root("root-prefix-but-sibling")))
        }

    @Test fun legacyRelationshipsFollowQueriedProviderEdges() =
        runBlocking {
            assertTrue(tree.descendantByTraversal(android.net.Uri.parse(root("root")), "nested-book"))
            assertFalse(tree.descendantByTraversal(android.net.Uri.parse(root("unrelated-looking-id")), "root"))
            assertFalse(tree.descendantByTraversal(android.net.Uri.parse(root("root")), "root-prefix-but-sibling"))
        }

    @Test fun legacyRelationshipsTerminateOnRepeatedDirectoryIds() =
        runBlocking {
            provider.cyclic = true
            assertFalse(tree.descendantByTraversal(android.net.Uri.parse(root("root")), "absent"))
            assertEquals(2, provider.childQueries)
        }

    @Test(expected = java.io.IOException::class)
    fun legacyRelationshipsRejectIncompleteEnumeration() =
        runBlocking {
            provider.loading = true
            tree.descendantByTraversal(android.net.Uri.parse(root("root")), "absent")
            Unit
        }

    @Test(expected = UnsupportedOperationException::class)
    fun unsupportedProviderRelationshipPropagatesInsteadOfClaimingNoOverlap() =
        runBlocking {
            provider.unsupportedRelationships = true
            tree.isChild(root("root"), root("unrelated-looking-id"))
            Unit
        }

    @Test fun walkIsLazyReadOnlyAndExcludesSubtreesWithRelativeDisplayPaths() =
        runBlocking {
            val flow = tree.walk(root("root"), setOf("unrelated-looking-id"))
            assertEquals(0, provider.childQueries)
            val files = flow.toList()
            assertEquals(listOf("Books/Novel.epub"), files.map { it.displayPath })
            assertEquals(listOf(root("root")), files.map { it.rootUri })
            assertEquals(1, provider.childQueries)
        }

    @Test(expected = java.io.IOException::class)
    fun incompleteProviderListingFailsInsteadOfCompleting() =
        runBlocking {
            provider.loading = true
            tree.walk(root("root"), emptySet()).toList()
            Unit
        }
}

class TreeProvider : DocumentsProvider() {
    var childQueries = 0
    var loading = false
    var cyclic = false
    var unsupportedRelationships = false

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(arrayOf("root_id"))

    private fun cursor() = MatrixCursor(arrayOf("document_id", "_display_name", "mime_type", "_size", "last_modified"))

    private fun MatrixCursor.document(
        id: String,
        name: String,
        directory: Boolean,
    ) {
        addRow(
            arrayOf<Any>(
                id,
                name,
                if (directory) DocumentsContract.Document.MIME_TYPE_DIR else "application/epub+zip",
                42L,
                5L,
            ),
        )
    }

    override fun queryDocument(
        documentId: String,
        projection: Array<out String>?,
    ): Cursor =
        cursor().apply {
            document(documentId, if (documentId == "root") "Books" else "Subfolder", true)
        }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor =
        cursor().apply {
            childQueries++
            if (cyclic) document(parentDocumentId, "Loop", true)
            if (parentDocumentId == "root") {
                document("book", "Novel.epub", false)
                document("unrelated-looking-id", "Nested", true)
            } else {
                document("nested-book", "Hidden.epub", false)
            }
            if (loading) extras = Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, true) }
        }

    override fun isChildDocument(
        parentDocumentId: String,
        documentId: String,
    ): Boolean {
        if (unsupportedRelationships) throw UnsupportedOperationException("No relationship operation")
        return parentDocumentId == "root" && documentId in setOf("book", "unrelated-looking-id", "nested-book") ||
            parentDocumentId == "unrelated-looking-id" && documentId == "nested-book"
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor = throw FileNotFoundException()
}
