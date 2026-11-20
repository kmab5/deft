@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.format.readium

import android.content.Context
import android.net.Uri
import com.kmab.deft.core.model.ReaderFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpStreamResponse
import org.readium.r2.shared.util.http.HttpTry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class ReaderOpenException(
    message: String,
) : IOException(message)

/** This client has no socket, request queue or fallback transport. */
object OfflineHttpClient : HttpClient {
    override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> =
        Try.failure(HttpError.IO(IOException("External resources are unavailable in the offline reader.")))
}

class OpenedEpub internal constructor(
    val publication: Publication,
    private val snapshot: File,
) : AutoCloseable {
    override fun close() {
        try {
            publication.close()
        } finally {
            ReaderSnapshots.discard(snapshot)
        }
    }
}

class OfflineEpubEngine(
    private val context: Context,
) {
    /** A private snapshot guarantees that saved locators belong to the bytes being displayed. */
    suspend fun open(
        uri: Uri,
        expectedHash: String,
        fonts: List<ReaderFont>,
    ): OpenedEpub {
        var result: OpenedEpub? = null
        var delivered = false
        try {
            val opened = withContext(Dispatchers.IO) { openSnapshot(uri, expectedHash, fonts).also { result = it } }
            delivered = true
            return opened
        } finally {
            // withContext can cancel while delivering an already opened publication to its caller.
            if (!delivered) result?.close()
        }
    }

    private suspend fun openSnapshot(
        uri: Uri,
        expectedHash: String,
        fonts: List<ReaderFont>,
    ): OpenedEpub {
        if (uri.scheme != "content") throw ReaderOpenException("Choose this book from a connected library folder.")
        val snapshot = ReaderSnapshots.create(File(context.cacheDir, "reader"))
        var publication: Publication? = null
        var retained = false
        try {
            copyVerified(uri, expectedHash, snapshot)
            publication = openPublication(snapshot, fontResources(fonts), bundledFontResources(fonts))
            if (publication.isRestricted || publication.isProtected) {
                throw ReaderOpenException("Protected books cannot be opened in Deft.")
            }
            val opened = OpenedEpub(publication, snapshot)
            retained = true
            return opened
        } finally {
            if (!retained) {
                try {
                    publication?.close()
                } finally {
                    ReaderSnapshots.discard(snapshot)
                }
            }
        }
    }

    private suspend fun copyVerified(
        uri: Uri,
        expectedHash: String,
        snapshot: File,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        val input =
            context.contentResolver.openInputStream(uri)
                ?: throw ReaderOpenException("This book is unavailable. Reconnect its folder and scan again.")
        input.use { source ->
            snapshot.outputStream().use { target ->
                copyAndHash(source, target, digest)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        if (actualHash != expectedHash) {
            throw ReaderOpenException("This file has changed. Scan its folder again before opening it.")
        }
    }

    private suspend fun copyAndHash(
        source: InputStream,
        target: OutputStream,
        digest: MessageDigest,
    ) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        while (true) {
            coroutineContext.ensureActive()
            val read = source.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
            target.write(buffer, 0, read)
        }
    }

    private suspend fun openPublication(
        snapshot: File,
        fonts: Map<Url, File>,
        bundledFonts: Map<Url, String>,
    ): Publication {
        val asset =
            AssetRetriever(context.contentResolver, OfflineHttpClient).retrieve(snapshot).getOrNull()
                ?: throw ReaderOpenException("This EPUB could not be read. The file may be damaged.")
        var retained = false
        try {
            val opener = PublicationOpener(EpubParser(httpClient = OfflineHttpClient), contentProtections = emptyList())
            val result =
                opener
                    .open(asset, allowUserInteraction = false, onCreatePublication = {
                        manifest =
                            manifest.copy(
                                resources =
                                    manifest.resources +
                                        fonts.map { (url, file) ->
                                            Link(
                                                href = url,
                                                mediaType =
                                                    MediaType(
                                                        if (file.extension ==
                                                            "otf"
                                                        ) {
                                                            "font/otf"
                                                        } else {
                                                            "font/ttf"
                                                        },
                                                    ),
                                            )
                                        } +
                                        bundledFonts.map { (url, path) ->
                                            Link(href = url, mediaType = MediaType(fontMediaType(path)))
                                        },
                            )
                        val links = manifest.readingOrder + manifest.resources
                        val html = links.filter { it.mediaType?.isHtml == true }.map { it.url() }.toSet()
                        val svg = links.filter { it.mediaType?.toString() == "image/svg+xml" }.map { it.url() }.toSet()
                        container = OfflineContainer(container, fonts, html, svg, context.assets, bundledFonts)
                    })
                    .getOrNull()
                    ?: throw ReaderOpenException("This EPUB could not be opened. It may be protected or damaged.")
            retained = true
            return result
        } finally {
            if (!retained) asset.close()
        }
    }

    private fun fontResources(fonts: List<ReaderFont>): Map<Url, File> =
        buildMap {
            val privateRoot = File(context.filesDir, "reader-fonts").canonicalFile.toPath()
            fonts.forEach { font ->
                font.faces.forEachIndexed { index, face ->
                    face.privateFilePath?.let { path ->
                        val file = File(path).canonicalFile
                        if (file.toPath().startsWith(privateRoot) && file.isFile) {
                            put(requireNotNull(Url(importedFontPath(font.id, index))), file)
                        }
                    }
                }
            }
        }

    private fun bundledFontResources(fonts: List<ReaderFont>): Map<Url, String> =
        buildMap {
            fonts.forEach { font ->
                font.faces.forEachIndexed { index, face ->
                    face.assetPath?.takeIf(PublicationPolicy::isLocalResource)?.let { path ->
                        put(requireNotNull(Url(importedFontPath(font.id, index))), path)
                    }
                }
            }
        }
}

private fun fontMediaType(path: String): String =
    when (path.substringAfterLast('.').lowercase()) {
        "woff2" -> "font/woff2"
        "woff" -> "font/woff"
        "otf" -> "font/otf"
        else -> "font/ttf"
    }

internal fun importedFontPath(
    id: String,
    face: Int,
): String = "__deft_fonts/${id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }}-$face.font"

/** Only the first open in this process removes leftovers; active readers keep their snapshots. */
internal object ReaderSnapshots {
    private val initialized = mutableSetOf<String>()
    private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun discard(snapshot: File) {
        if (snapshot.delete() || !snapshot.exists()) return
        // Readium closes ZIP handles asynchronously; some filesystems forbid deleting open files.
        cleanup.launch {
            repeat(DELETE_RETRIES) {
                delay(DELETE_RETRY_DELAY_MILLIS)
                if (snapshot.delete() || !snapshot.exists()) return@launch
            }
        }
    }

    @Synchronized fun create(directory: File): File {
        directory.mkdirs()
        if (initialized.add(directory.canonicalPath)) {
            directory
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile && it.name.startsWith("epub-") && it.extension == "epub"
                }.forEach { it.delete() }
        }
        return File.createTempFile("epub-", ".epub", directory)
    }
}

private const val COPY_BUFFER_BYTES = 65_536
private const val DELETE_RETRIES = 20
private const val DELETE_RETRY_DELAY_MILLIS = 50L
