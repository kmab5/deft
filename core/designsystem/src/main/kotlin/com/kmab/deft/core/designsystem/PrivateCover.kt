package com.kmab.deft.core.designsystem

import java.io.File
import java.io.IOException
import java.nio.file.InvalidPathException

/** Only app-private files enter image loaders; metadata strings never become remote URLs. */
fun privateCoverFile(
    path: String?,
    root: File,
): File? {
    if (path == null) return null
    return try {
        val candidate = File(path)
        candidate.takeIf { it.isAbsolute }?.canonicalFile?.takeIf {
            it.toPath().startsWith(
                root.canonicalFile.toPath(),
            )
        }
    } catch (_: IOException) {
        null
    } catch (_: InvalidPathException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
