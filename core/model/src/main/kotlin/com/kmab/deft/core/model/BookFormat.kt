package com.kmab.deft.core.model

enum class LayoutKind { REFLOWABLE, FIXED }

enum class BookFormat(
    val displayName: String,
    val extensions: List<String>,
    val layout: LayoutKind,
) {
    EPUB("EPUB", listOf("epub"), LayoutKind.REFLOWABLE),
    MOBI("MOBI", listOf("mobi", "prc"), LayoutKind.REFLOWABLE),
    AZW("AZW", listOf("azw"), LayoutKind.REFLOWABLE),
    AZW3("AZW3", listOf("azw3", "kf8"), LayoutKind.REFLOWABLE),
    FB2("FB2", listOf("fb2", "fb2.zip"), LayoutKind.REFLOWABLE),
    TXT("TXT", listOf("txt"), LayoutKind.REFLOWABLE),
    PDF("PDF", listOf("pdf"), LayoutKind.FIXED),
    DJVU("DjVu", listOf("djvu", "djv"), LayoutKind.FIXED),
    CBZ("CBZ", listOf("cbz"), LayoutKind.FIXED),
    ;

    companion object {
        private val extensionsByLength =
            entries
                .flatMap { format -> format.extensions.map { extension -> extension to format } }
                .sortedByDescending { (extension, _) -> extension.length }

        fun fromFileName(name: String): BookFormat? =
            extensionsByLength
                .firstOrNull { (extension, _) -> name.endsWith(".$extension", ignoreCase = true) }
                ?.second
    }
}
