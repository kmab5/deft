package com.kmab.deft.feature.readersettings

import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderFontFace
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** A private font and its label are published only after bounded copy and validation succeed. */
internal class ImportedFontStore(
    private val directory: File,
) {
    fun load(): List<ReaderFont> =
        directory
            .listFiles()
            .orEmpty()
            .filter { it.extension == "label" && ID.matches(it.nameWithoutExtension) }
            .mapNotNull(::readFont)
            .sortedBy { it.name.lowercase(java.util.Locale.ROOT) }

    fun importFont(
        input: InputStream,
        displayName: String,
        validate: (File) -> Unit = {},
    ): ReaderFont {
        if (load().size >= MAX_FONTS) throw IOException("Remove an imported font before adding another.")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Font storage is unavailable.")
        val id = UUID.randomUUID().toString()
        val pending = File(directory, "$id.part")
        var destination: File? = null
        val label = File(directory, "$id.label")
        val pendingLabel = File(directory, "$id.label.part")
        var published = false
        try {
            copyBounded(input, pending)
            val extension = validateSfnt(pending)
            validate(pending)
            destination = File(directory, "$id.$extension")
            val name =
                displayName
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .substringBeforeLast('.')
                    .filterNot(Char::isISOControl)
                    .trim()
                    .take(MAX_NAME)
                    .ifEmpty { "imported font" }
            pendingLabel.writeText(name, Charsets.UTF_8)
            Files.move(pending.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
            Files.move(pendingLabel.toPath(), label.toPath(), StandardCopyOption.ATOMIC_MOVE)
            published = true
            return font(id, name, destination)
        } finally {
            if (!published) {
                destination?.delete()
                label.delete()
            }
            pending.delete()
            pendingLabel.delete()
        }
    }

    fun remove(id: String) {
        require(ID.matches(id)) { "Invalid imported font." }
        listOf("label", "ttf", "otf").forEach { extension ->
            val file = File(directory, "$id.$extension")
            if (file.exists() && !file.delete()) throw IOException("The imported font could not be removed.")
        }
    }

    private fun readFont(label: File): ReaderFont? {
        val id = label.nameWithoutExtension
        val file = listOf("ttf", "otf").map { File(directory, "$id.$it") }.firstOrNull(File::isFile)
        return try {
            if (file == null || label.length() > MAX_LABEL_BYTES) {
                null
            } else {
                validateSfnt(file)
                val name = label.readText(Charsets.UTF_8).trim().take(MAX_NAME)
                if (name.isBlank()) null else font(id, name, file)
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun copyBounded(
        input: InputStream,
        target: File,
    ) {
        target.outputStream().use { output ->
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0L
            var count = input.read(bytes)
            while (count != -1) {
                copied += count
                if (copied > MAX_BYTES) throw IOException("Choose a font smaller than 20 MB.")
                output.write(bytes, 0, count)
                count = input.read(bytes)
            }
        }
    }

    private fun font(
        id: String,
        name: String,
        file: File,
    ) = ReaderFont(id, name, listOf(ReaderFontFace(privateFilePath = file.absolutePath)), imported = true)

    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        private const val MAX_FONTS = 32
        private const val MAX_NAME = 64
        private const val MAX_LABEL_BYTES = 512
        private val ID = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    }
}

/** Reject collections, truncated/oversized tables and arbitrary files before invoking the native font parser. */
private fun validateSfnt(file: File): String {
    if (file.length() !in SFNT_HEADER_SIZE..ImportedFontStore.MAX_BYTES.toLong()) invalidFont()
    return RandomAccessFile(file, "r").use { input ->
        val extension =
            when (input.readInt()) {
                TRUETYPE_SIGNATURE -> "ttf"
                OPENTYPE_SIGNATURE -> "otf"
                else -> invalidFont()
            }
        val tableCount = input.readUnsignedShort()
        if (tableCount !in 1..MAX_TABLES) invalidFont()
        val directoryEnd = SFNT_HEADER_SIZE + tableCount * TABLE_ENTRY_SIZE
        if (directoryEnd > file.length()) invalidFont()
        input.seek(SFNT_HEADER_SIZE)
        val tables = mutableSetOf<String>()
        repeat(tableCount) {
            readSfntTable(input, directoryEnd, file.length())?.let(tables::add)
        }
        if (!tables.containsAll(REQUIRED_TABLES)) invalidFont()
        if (!(tables.containsAll(setOf("glyf", "loca")) || "CFF " in tables || "CFF2" in tables)) invalidFont()
        extension
    }
}

private fun readSfntTable(
    input: RandomAccessFile,
    directoryEnd: Long,
    fileLength: Long,
): String? {
    val tag = ByteArray(TAG_SIZE).also(input::readFully).toString(Charsets.US_ASCII)
    input.readInt()
    val offset = Integer.toUnsignedLong(input.readInt())
    val length = Integer.toUnsignedLong(input.readInt())
    if (offset < directoryEnd || offset + length > fileLength) invalidFont()
    return tag.takeIf { length > 0 }
}

private fun invalidFont(): Nothing = throw IOException("Choose a valid TTF or OTF font file.")

private const val SFNT_HEADER_SIZE = 12L
private const val TABLE_ENTRY_SIZE = 16
private const val TAG_SIZE = 4
private const val MAX_TABLES = 1024
private const val TRUETYPE_SIGNATURE = 0x00010000
private const val OPENTYPE_SIGNATURE = 0x4f54544f
private val REQUIRED_TABLES = setOf("cmap", "head", "hhea", "hmtx", "maxp", "name")
