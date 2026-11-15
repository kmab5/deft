package com.kmab.deft.format.epub

import com.kmab.deft.core.model.MetadataException
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.CancellationException

class TemporaryEpubTest {
    @Test fun `cleanup failure becomes metadata exception`() {
        val path = Files.createTempDirectory("deft-cleanup-test")
        val child = Files.createFile(path.resolve("occupied"))
        try {
            assertThrows(MetadataException::class.java) { TemporaryEpub(path).close() }
        } finally {
            Files.delete(child)
            Files.delete(path)
        }
    }

    @Test fun `cleanup does not replace cancellation`() {
        val path = Files.createTempDirectory("deft-cleanup-test")
        val child = Files.createFile(path.resolve("occupied"))
        val cancellation = CancellationException("stop")
        try {
            val thrown =
                assertThrows(CancellationException::class.java) {
                    TemporaryEpub(path).use { throw cancellation }
                }
            assertSame(cancellation, thrown)
            assertSame(MetadataException::class.java, thrown.suppressed.single().javaClass)
        } finally {
            Files.delete(child)
            Files.delete(path)
        }
    }
}
