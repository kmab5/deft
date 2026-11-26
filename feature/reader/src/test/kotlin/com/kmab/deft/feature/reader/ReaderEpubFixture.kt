package com.kmab.deft.feature.reader

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun navigatorEpubBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        val entries =
            listOf(
                "META-INF/container.xml",
                "EPUB/package.opf",
                "EPUB/one.xhtml",
                "EPUB/two.xhtml",
                "EPUB/nav.xhtml",
            )
        val mimetype = "application/epub+zip".toByteArray()
        zip.putNextEntry(
            ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimetype.size.toLong()
                compressedSize = size
                crc = CRC32().apply { update(mimetype) }.value
            },
        )
        zip.write(mimetype)
        zip.closeEntry()
        entries.forEach { name ->
            zip.putNextEntry(ZipEntry(name))
            requireNotNull(ReaderSession::class.java.classLoader?.getResourceAsStream("navigator-epub/$name")).use {
                it.copyTo(zip)
            }
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}
