package com.kmab.deft.format.epub

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TestEpubBuilder {
    private val metadata = StringBuilder()
    private val entries = linkedMapOf<String, ByteArray>()
    var opfPath = "OPS/package.opf"
    var coverHref = "images/cover.jpg"
    var epub2Cover = false
    var reverseEntries = false
    var rawOpf: String? = null

    fun title(value: String) = apply { metadata.append("<dc:title>${xml(value)}</dc:title>") }

    fun creator(
        value: String,
        fileAs: String? = null,
    ) = apply {
        metadata.append("<dc:creator opf:file-as=\"${xml(fileAs.orEmpty())}\">${xml(value)}</dc:creator>")
    }

    fun calibreSeries(
        name: String,
        index: Double,
    ) = apply {
        metadata.append("<meta name=\"calibre:series\" content=\"${xml(name)}\"/>")
        metadata.append("<meta name=\"calibre:series_index\" content=\"$index\"/>")
    }

    fun collection(
        name: String,
        index: Double,
    ) = apply {
        metadata.append("<meta property=\"belongs-to-collection\" id=\"series\">${xml(name)}</meta>")
        metadata.append("<meta refines=\"#series\" property=\"collection-type\">series</meta>")
        metadata.append("<meta refines=\"#series\" property=\"group-position\">$index</meta>")
    }

    fun isbn(value: String) = apply { metadata.append("<dc:identifier>${xml(value)}</dc:identifier>") }

    fun language(value: String) = apply { metadata.append("<dc:language>${xml(value)}</dc:language>") }

    fun description(value: String) = apply { metadata.append("<dc:description>${xml(value)}</dc:description>") }

    fun cover(bytes: ByteArray) = apply { entries["OPS/images/cover.jpg"] = bytes }

    fun entry(
        path: String,
        bytes: ByteArray,
    ) = apply { entries[path] = bytes }

    fun encryption(algorithm: String) =
        entry(
            "META-INF/encryption.xml",
            """
            <encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <EncryptedData xmlns="http://www.w3.org/2001/04/xmlenc#">
                <EncryptionMethod Algorithm="${xml(algorithm)}"/>
              </EncryptedData>
            </encryption>
            """.trimIndent().toByteArray(),
        )

    fun build(): ByteArray {
        val container =
            """
            <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles>
              <rootfile full-path="$opfPath" media-type="application/oebps-package+xml"/>
            </rootfiles></container>
            """.trimIndent()
        val coverMeta = if (epub2Cover) "<meta name=\"cover\" content=\"cover\"/>" else ""
        val properties = if (epub2Cover) "" else "cover-image"
        val opf =
            """
            <package xmlns="http://www.idpf.org/2007/opf"
                     xmlns:opf="http://www.idpf.org/2007/opf"
                     xmlns:dc="http://purl.org/dc/elements/1.1/">
              <metadata>$metadata$coverMeta</metadata>
              <manifest><item id="cover" href="$coverHref" media-type="image/jpeg"
                              properties="$properties"/></manifest>
            </package>
            """.trimIndent()
        val files =
            linkedMapOf(
                "mimetype" to "application/epub+zip".toByteArray(),
                "META-INF/container.xml" to container.toByteArray(),
                opfPath to (rawOpf ?: opf).toByteArray(),
            )
        files.putAll(entries)
        return ByteArrayOutputStream()
            .also { output ->
                ZipOutputStream(output).use { zip ->
                    val ordered = if (reverseEntries) files.entries.reversed() else files.entries.toList()
                    ordered.forEach { (name, bytes) ->
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
            }.toByteArray()
    }

    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
}
