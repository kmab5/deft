package com.kmab.deft.format.readium

import android.content.res.AssetManager
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.resource.InMemoryResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import java.io.File

internal class OfflineContainer(
    private val source: Container<Resource>,
    private val fonts: Map<Url, File>,
    private val html: Set<Url>,
    private val svg: Set<Url>,
    private val assets: AssetManager,
    private val bundledFonts: Map<Url, String>,
) : Container<Resource> {
    override val entries: Set<Url> get() = source.entries + fonts.keys + bundledFonts.keys

    // Reject unsafe paths before choosing a private font or a read-only book resource.
    @Suppress("ReturnCount")
    override fun get(url: Url): Resource? {
        if (!PublicationPolicy.isLocalResource(url.toString())) return null
        bundledFonts[url]?.let { path ->
            return InMemoryResource(null, Resource.Properties()) {
                try {
                    Try.success(assets.open(path).use { it.readBytes() })
                } catch (error: java.io.IOException) {
                    Try.failure(ReadError.Decoding(error))
                }
            }
        }
        val font = fonts[url]
        return if (font != null) FileResource(font) else source[url]?.let { sanitized(url, it) }
    }

    private fun sanitized(
        url: Url,
        resource: Resource,
    ): Resource {
        val extension =
            url
                .toString()
                .substringBefore('#')
                .substringBefore('?')
                .substringAfterLast('.')
                .lowercase()
        return when {
            url in html || extension in setOf("html", "htm", "xhtml") ->
                TransformingResource(resource) { Try.success(PublicationPolicy.sanitizeHtml(it).toByteArray()) }
            url in svg || extension == "svg" ->
                TransformingResource(resource) { Try.success(PublicationPolicy.sanitizeSvg(it).toByteArray()) }
            else -> resource
        }
    }

    override fun close() = source.close()
}
