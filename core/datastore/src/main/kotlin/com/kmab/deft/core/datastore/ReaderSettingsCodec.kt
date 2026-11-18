package com.kmab.deft.core.datastore

import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.core.model.ReaderThemes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object ReaderSettingsCodec {
    fun decode(raw: String?): ReaderPreferences =
        try {
            val json = JSONObject(raw ?: "{}")
            val defaults = ReaderPreferences()
            ReaderPreferences(
                fontFamily = json.optString("fontFamily", defaults.fontFamily),
                fontSize = json.optDouble("fontSize", defaults.fontSize),
                lineHeight = json.optDouble("lineHeight", defaults.lineHeight),
                margins = ReaderMargins.entries.find { it.name == json.optString("margins") } ?: defaults.margins,
                alignment =
                    ReaderAlignment.entries.find { it.name == json.optString("alignment") } ?: defaults.alignment,
                hyphenation = json.optBoolean("hyphenation", defaults.hyphenation),
                themeId = json.optString("themeId", defaults.themeId),
                followSystem = json.optBoolean("followSystem", defaults.followSystem),
                lightThemeId = json.optString("lightThemeId", defaults.lightThemeId),
                darkThemeId = json.optString("darkThemeId", defaults.darkThemeId),
                brightness = json.optDouble("brightness", defaults.brightness.toDouble()).toFloat(),
                useSystemBrightness = json.optBoolean("useSystemBrightness", defaults.useSystemBrightness),
                scroll = json.optBoolean("scroll", defaults.scroll),
                pageAnimation =
                    PageAnimation.entries.find { it.name == json.optString("pageAnimation") } ?: defaults.pageAnimation,
                showStatusLine = json.optBoolean("showStatusLine", defaults.showStatusLine),
                volumeKeys = json.optBoolean("volumeKeys", defaults.volumeKeys),
                keepScreenOn = json.optBoolean("keepScreenOn", defaults.keepScreenOn),
            ).normalized()
        } catch (_: JSONException) {
            ReaderPreferences()
        }

    fun encode(value: ReaderPreferences): String =
        JSONObject()
            .apply {
                put("fontFamily", value.fontFamily)
                put("fontSize", value.fontSize)
                put("lineHeight", value.lineHeight)
                put("margins", value.margins.name)
                put("alignment", value.alignment.name)
                put("hyphenation", value.hyphenation)
                put("themeId", value.themeId)
                put("followSystem", value.followSystem)
                put("lightThemeId", value.lightThemeId)
                put("darkThemeId", value.darkThemeId)
                put("brightness", value.brightness.toDouble())
                put("useSystemBrightness", value.useSystemBrightness)
                put("scroll", value.scroll)
                put("pageAnimation", value.pageAnimation.name)
                put("showStatusLine", value.showStatusLine)
                put("volumeKeys", value.volumeKeys)
                put("keepScreenOn", value.keepScreenOn)
            }.toString()

    fun themes(raw: String?): List<ReaderTheme> =
        try {
            val entries = JSONArray(raw ?: "[]")
            buildList {
                for (index in 0 until minOf(entries.length(), MAX_THEMES)) {
                    val json = entries.optJSONObject(index) ?: continue
                    val theme =
                        ReaderTheme(
                            json.optString("id"),
                            json.optString("name"),
                            json.optString("background"),
                            json.optString("text"),
                            json.optString("accent"),
                        )
                    if (ReaderThemes.validationError(theme) == null && none { it.id == theme.id }) add(theme)
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }

    fun encodeThemes(themes: List<ReaderTheme>): String =
        JSONArray()
            .apply {
                themes.forEach { theme ->
                    put(
                        JSONObject()
                            .put("id", theme.id)
                            .put("name", theme.name)
                            .put("background", theme.background)
                            .put("text", theme.text)
                            .put("accent", theme.accent),
                    )
                }
            }.toString()

    private const val MAX_THEMES = 32
}
