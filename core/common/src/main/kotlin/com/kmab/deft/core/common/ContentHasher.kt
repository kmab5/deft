package com.kmab.deft.core.common

import com.kmab.deft.core.model.ContentHash
import java.io.InputStream
import java.security.MessageDigest

/** Hashes content from the current position to EOF without closing the caller-owned stream. */
class ContentHasher {
    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }

    fun hash(input: InputStream): ContentHash {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            digest.update(buffer, 0, count)
        }
        return ContentHash(digest.digest().joinToString("") { "%02x".format(it) })
    }
}
