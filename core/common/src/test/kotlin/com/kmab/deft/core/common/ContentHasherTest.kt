package com.kmab.deft.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

class ContentHasherTest {
    @Test
    fun `empty content has the standard SHA-256 hash`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ContentHasher().hash(byteArrayOf().inputStream()).hex,
        )
    }

    @Test
    fun `deft content matches SHA-256`() {
        val bytes = "deft".toByteArray()
        assertEquals(
            MessageDigest.getInstance("SHA-256").digest(bytes).toHex(),
            ContentHasher().hash(bytes.inputStream()).hex,
        )
    }

    @Test
    fun `ten megabytes are hashed incrementally with bounded reads`() {
        val size = 10 * 1024 * 1024
        val expected = MessageDigest.getInstance("SHA-256")
        repeat(size / 1024) { expected.update(ByteArray(1024) { 42 }) }
        val stream =
            object : InputStream() {
                var remaining = size

                override fun read(): Int = error("Bulk reads required")

                override fun read(
                    bytes: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int {
                    assertTrue(length <= 64 * 1024)
                    if (remaining == 0) return -1
                    val count = minOf(length, remaining, 997)
                    bytes.fill(42, offset, offset + count)
                    remaining -= count
                    return count
                }
            }
        assertEquals(expected.digest().toHex(), ContentHasher().hash(stream).hex)
        assertEquals(0, stream.remaining)
    }

    @Test
    fun `caller retains stream ownership after successful hashing`() {
        val stream =
            object : java.io.ByteArrayInputStream("deft".toByteArray()) {
                var closed = false

                override fun close() {
                    closed = true
                }
            }
        ContentHasher().hash(stream)
        assertFalse(stream.closed)
    }

    @Test
    fun `read failures propagate without closing caller stream`() {
        val failure = IOException("read failed")
        val stream =
            object : InputStream() {
                var closed = false

                override fun read(): Int = throw failure

                override fun close() {
                    closed = true
                }
            }
        assertSame(failure, assertThrows(IOException::class.java) { ContentHasher().hash(stream) })
        assertFalse(stream.closed)
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
