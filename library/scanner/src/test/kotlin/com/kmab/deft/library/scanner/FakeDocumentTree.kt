package com.kmab.deft.library.scanner

import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.io.InputStream

class FakeDocumentTree : DocumentTree {
    val files = linkedMapOf<String, Pair<DocumentEntry, ByteArray>>()
    val unavailable = mutableSetOf<String>()
    val failWalk = mutableSetOf<String>()
    val failOpen = mutableSetOf<String>()
    val parents = mutableMapOf<String, String>()
    val ancestors = mutableMapOf<String, Set<String>>()
    var relationshipFailure: Exception? = null
    var opens = 0
    var onRead: (() -> Unit)? = null
    var interruption: RuntimeException? = null

    fun put(
        uri: String,
        root: String = "R",
        bytes: ByteArray = uri.toByteArray(),
        stamp: Long = 1,
    ) {
        files[uri] = DocumentEntry(uri, root, uri, "$uri.epub", bytes.size.toLong(), stamp) to bytes
    }

    override suspend fun isAccessible(rootUri: String) = rootUri !in unavailable

    override suspend fun isChild(
        parentRootUri: String,
        childRootUri: String,
    ): Boolean {
        relationshipFailure?.let { throw it }
        return parents[childRootUri] == parentRootUri
    }

    override fun walk(
        rootUri: String,
        excludedDocumentIds: Set<String>,
    ) = flow {
        files.values.filter { it.first.rootUri == rootUri }.forEach { (entry, _) ->
            if (ancestors[entry.uri].orEmpty().intersect(excludedDocumentIds).isEmpty()) emit(entry)
            interruption?.let { throw it }
        }
        if (rootUri in failWalk) throw IOException("Incomplete enumeration")
    }

    override fun open(uri: String): InputStream {
        opens++
        if (uri in failOpen) throw IOException("Cannot open")
        return object : java.io.FilterInputStream(files.getValue(uri).second.inputStream()) {
            override fun read(
                bytes: ByteArray,
                offset: Int,
                length: Int,
            ): Int {
                onRead?.invoke()
                return super.read(bytes, offset, length)
            }
        }
    }
}
