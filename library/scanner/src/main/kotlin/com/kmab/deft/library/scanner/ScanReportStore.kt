package com.kmab.deft.library.scanner

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/** Named scan diagnostics live only in app storage, never in WorkManager's small output payload. */
@Singleton
class ScanReportStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val directory = File(context.filesDir, "scan-reports")

        fun observe(reportId: String): Flow<ScanDetails?> = revisions.map { load(reportId) }.distinctUntilChanged()

        suspend fun load(reportId: String): ScanDetails? =
            withContext(Dispatchers.IO) {
                if (!VALID_ID.matches(reportId)) return@withContext null
                lock.withLock { read(reportId) }
            }

        internal suspend fun append(
            reportId: String,
            details: ScanDetails,
        ) = withContext(Dispatchers.IO) {
            require(VALID_ID.matches(reportId)) { "Invalid scan report ID" }
            lock.withLock {
                val records = ScanDetailBuffer()
                read(reportId)?.let(records::add)
                records.add(details)
                write(reportId, records.snapshot())
                revisions.value += 1
                retainRecent(reportId)
            }
        }

        private fun read(reportId: String): ScanDetails? {
            val file = File(directory, "$reportId.json")
            if (!file.isFile || file.length() > MAX_REPORT_BYTES) return null
            return try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val rows = json.getJSONArray("entries")
                val records = ScanDetailBuffer()
                for (index in 0 until rows.length()) records.add(decode(rows.getJSONObject(index)))
                val bounded = records.snapshot()
                bounded.copy(omittedCount = bounded.omittedCount + json.optInt("omittedCount"))
            } catch (_: IOException) {
                null
            } catch (_: JSONException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        private suspend fun write(
            reportId: String,
            details: ScanDetails,
        ) {
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create scan report directory")
            val target = File(directory, "$reportId.json")
            val temporary = File(directory, "$reportId.tmp")
            try {
                val rows = JSONArray()
                var bytes = JSON_HEADER_BUDGET
                var omitted = details.omittedCount
                for (entry in details.entries) {
                    currentCoroutineContext().ensureActive()
                    val encoded = encode(entry)
                    val size = encoded.toString().toByteArray(Charsets.UTF_8).size + 1
                    if (bytes + size <= MAX_REPORT_BYTES) {
                        rows.put(encoded)
                        bytes += size
                    } else {
                        omitted++
                    }
                }
                val json = JSONObject().put("entries", rows).put("omittedCount", omitted)
                temporary.outputStream().use { stream ->
                    stream.write(json.toString().toByteArray(Charsets.UTF_8))
                    stream.fd.sync()
                }
                currentCoroutineContext().ensureActive()
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } finally {
                Files.deleteIfExists(temporary.toPath())
            }
        }

        private fun retainRecent(current: String) {
            val ownedFiles = directory.listFiles().orEmpty().filter { VALID_ID.matches(it.nameWithoutExtension) }
            val newestFirst =
                compareByDescending<File> { it.nameWithoutExtension == current }
                    .thenByDescending { it.lastModified() }
            ownedFiles.filter { it.extension == "tmp" }.forEach { Files.deleteIfExists(it.toPath()) }
            ownedFiles
                .filter { it.extension == "json" }
                .sortedWith(newestFirst)
                .drop(RETAINED_REPORTS)
                .forEach { Files.deleteIfExists(it.toPath()) }
        }

        private fun encode(entry: ScanDetailEntry): JSONObject =
            JSONObject()
                .put("id", entry.id)
                .put("name", entry.name)
                .put("path", entry.path)
                .put("outcome", entry.outcome.name)
                .put("reason", entry.reason)

        private fun decode(json: JSONObject): ScanDetailEntry =
            ScanDetailEntry(
                json.getString("id"),
                json.getString("name"),
                json.getString("path"),
                ScanDetailOutcome.valueOf(json.getString("outcome")),
                json.getString("reason"),
            )

        private companion object {
            val lock = Mutex()
            val revisions = MutableStateFlow(0L)
            val VALID_ID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            const val RETAINED_REPORTS = 3
            const val MAX_REPORT_BYTES = 8L * 1024 * 1024
            const val JSON_HEADER_BUDGET = 1024L
        }
    }
