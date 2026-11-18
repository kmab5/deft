package com.kmab.deft.library.scanner

import android.content.SharedPreferences
import org.json.JSONArray
import java.io.IOException

/** Small durable totals for one logical WorkRequest; retries rewalk committed fingerprints. */
internal class ScanContinuationStore(
    private val preferences: SharedPreferences,
) {
    fun combine(
        workId: String,
        report: ScanReport,
    ): ScanReport {
        if (preferences.getString("workId", null) != workId) return report
        val details = JSONArray(preferences.getString("details", "[]"))
        return report.copy(
            added = report.added + preferences.getInt("added", 0),
            updated = report.updated + preferences.getInt("updated", 0),
            reattached = report.reattached + preferences.getInt("reattached", 0),
            markedMissing = report.markedMissing + preferences.getInt("markedMissing", 0),
            errors = report.errors + preferences.getInt("errors", 0),
            metadataFallbacks = report.metadataFallbacks + preferences.getInt("metadataFallbacks", 0),
            errorDetails =
                (
                    (0 until details.length()).map {
                        details.getString(
                            it,
                        )
                    } + report.errorDetails
                ).distinct().take(MAX_ERROR_DETAILS),
        )
    }

    fun save(
        workId: String,
        report: ScanReport,
    ) {
        val saved =
            preferences
                .edit()
                .clear()
                .putString("workId", workId)
                .putInt("added", report.added)
                .putInt("updated", report.updated)
                .putInt("reattached", report.reattached)
                .putInt("markedMissing", report.markedMissing)
                .putInt("errors", report.errors)
                .putInt("metadataFallbacks", report.metadataFallbacks)
                .putString("details", JSONArray(report.errorDetails).toString())
                .commit()
        if (!saved) throw IOException("Cannot save scan continuation")
    }

    private companion object {
        const val MAX_ERROR_DETAILS = 8
    }
}
