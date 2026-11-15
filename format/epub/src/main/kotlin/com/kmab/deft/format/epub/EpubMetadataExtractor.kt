package com.kmab.deft.format.epub

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ExtractedMetadata
import com.kmab.deft.core.model.MetadataException
import com.kmab.deft.core.model.MetadataExtractor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipFile

/** Bounded private spool permits any ZIP entry order; ZIP paths are never extracted. */
class EpubMetadataExtractor : MetadataExtractor {
    override val formats = setOf(BookFormat.EPUB)

    override fun extract(
        input: InputStream,
        fileName: String,
    ): ExtractedMetadata {
        try {
            return TemporaryEpub(Files.createTempFile("deft-epub-", ".zip")).use { spool ->
                Files.newOutputStream(spool.path).use { output -> copyBounded(input, MAX_ARCHIVE_BYTES, output::write) }
                ZipFile(spool.path.toFile()).use(::extractZip)
            }
        } catch (error: IOException) {
            throw MetadataException("Cannot read EPUB metadata", error)
        } catch (error: org.xmlpull.v1.XmlPullParserException) {
            throw MetadataException("Invalid EPUB XML", error)
        } catch (error: org.w3c.dom.DOMException) {
            throw MetadataException("Invalid EPUB XML structure", error)
        } catch (error: java.net.URISyntaxException) {
            throw MetadataException("Invalid EPUB path", error)
        }
    }

    private fun extractZip(zip: ZipFile): ExtractedMetadata {
        val container = parseXml(read(zip, "META-INF/container.xml"))
        val opfPath =
            container
                .elements(CONTAINER_NS, "rootfile")
                .firstOrNull {
                    it.getAttribute("media-type") == "application/oebps-package+xml"
                }?.getAttribute("full-path")
                ?.takeIf(String::isNotBlank)
                ?: throw MetadataException("Missing EPUB package location")
        val opf = OpfParser(read(zip, opfPath))
        val cover =
            opf.coverHref?.let { href ->
                val path = coverPath(opfPath, href)
                zip.getEntry(path)?.let { read(zip, path, MAX_COVER_BYTES) }
            }
        return opf.metadata.copy(coverImage = cover, drmProtected = protected(zip))
    }

    private fun coverPath(
        opfPath: String,
        href: String,
    ): String {
        val resolved = URI(null, null, opfPath, null).resolve(URI(href)).normalize()
        if (resolved.isAbsolute || resolved.rawAuthority != null || invalidArchivePath(resolved.path)) {
            throw MetadataException("Invalid EPUB cover path")
        }
        return resolved.path
    }

    private fun invalidArchivePath(path: String) =
        path.startsWith("/") ||
            path.startsWith("../")

    private fun protected(zip: ZipFile): Boolean {
        if (zip.getEntry("META-INF/rights.xml") != null || zip.getEntry("META-INF/license.lcpl") != null) return true
        return zip.getEntry("META-INF/encryption.xml") != null &&
            parseXml(read(zip, "META-INF/encryption.xml"))
                .elements("http://www.w3.org/2001/04/xmlenc#", "EncryptionMethod")
                .any { it.getAttribute("Algorithm") !in FONT_OBFUSCATION }
    }

    private fun read(
        zip: ZipFile,
        path: String,
        limit: Long = MAX_XML_BYTES,
    ): ByteArray {
        val entry = zip.getEntry(path) ?: throw MetadataException("Missing EPUB entry: $path")
        if (entry.size > limit) throw MetadataException("EPUB entry exceeds size limit: $path")
        return ByteArrayOutputStream().use { output ->
            zip.getInputStream(entry).use { copyBounded(it, limit, output::write) }
            output.toByteArray()
        }
    }

    private fun copyBounded(
        input: InputStream,
        limit: Long,
        write: (ByteArray, Int, Int) -> Unit,
    ) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return
            total += count
            if (total > limit) throw MetadataException("EPUB exceeds size limit")
            write(buffer, 0, count)
        }
    }

    private companion object {
        const val COPY_BUFFER_BYTES = 8192
        const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024
        const val MAX_XML_BYTES = 4L * 1024 * 1024
        const val MAX_COVER_BYTES = 20L * 1024 * 1024
        const val CONTAINER_NS = "urn:oasis:names:tc:opendocument:xmlns:container"
        val FONT_OBFUSCATION = setOf("http://www.idpf.org/2008/embedding", "http://ns.adobe.com/pdf/enc#RC")
    }
}
