package com.kmab.deft.format.readium

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import java.io.File

internal class OfflineContainer(
    private val source: Container<Resource>,
    private val fonts: Map<Url, File>,
    private val html: Set<Url>,
    private val svg: Set<Url>,
) : Container<Resource> {
    override val entries: Set<Url> get() = source.entries + fonts.keys

    override fun get(url: Url): Resource? {
        if (!PublicationPolicy.isLocalResource(url.toString())) return null
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
