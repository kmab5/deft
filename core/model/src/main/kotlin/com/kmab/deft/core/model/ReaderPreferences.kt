package com.kmab.deft.core.model

enum class ReaderMargins { SMALL, MEDIUM, LARGE }

enum class ReaderAlignment { START, JUSTIFY, ORIGINAL }

enum class PageAnimation { NONE, SLIDE }

enum class ReaderOrientation { SYSTEM, PORTRAIT, LANDSCAPE }

data class ReaderPreferences(
    val fontFamily: String = "Literata",
    val fontSize: Double = 1.0,
    val lineHeight: Double = DEFAULT_LINE_HEIGHT,
    val margins: ReaderMargins = ReaderMargins.MEDIUM,
    val alignment: ReaderAlignment = ReaderAlignment.START,
    val hyphenation: Boolean = true,
    val themeId: String = "light",
    val followSystem: Boolean = false,
    val lightThemeId: String = "light",
    val darkThemeId: String = "dark",
    val brightness: Float = DEFAULT_BRIGHTNESS,
    val useSystemBrightness: Boolean = true,
    val scroll: Boolean = false,
    val pageAnimation: PageAnimation = PageAnimation.SLIDE,
    val showStatusLine: Boolean = true,
    val volumeKeys: Boolean = false,
    val keepScreenOn: Boolean = true,
    val openLastBook: Boolean = false,
    val showPageNumber: Boolean = false,
    val fullScreen: Boolean = true,
    val tapToTurn: Boolean = true,
    val orientation: ReaderOrientation = ReaderOrientation.SYSTEM,
) {
    fun normalized(): ReaderPreferences =
        copy(
            fontFamily = fontFamily.trim().take(MAX_FONT_NAME).ifBlank { "Literata" },
            fontSize = if (fontSize.isFinite()) fontSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE) else 1.0,
            lineHeight =
                if (lineHeight.isFinite()) {
                    lineHeight.coerceIn(
                        MIN_LINE_HEIGHT,
                        MAX_LINE_HEIGHT,
                    )
                } else {
                    DEFAULT_LINE_HEIGHT
                },
            brightness = if (brightness.isFinite()) brightness.coerceIn(0f, 1f) else DEFAULT_BRIGHTNESS,
        )

    fun activeTheme(
        systemDark: Boolean,
        customThemes: List<ReaderTheme> = emptyList(),
    ): ReaderTheme {
        val id =
            if (followSystem) {
                if (systemDark) {
                    darkThemeId
                } else {
                    lightThemeId
                }
            } else {
                themeId
            }
        return ReaderThemes.find(id, customThemes) ?: ReaderThemes.builtIn.first()
    }

    companion object {
        const val MIN_FONT_SIZE = 0.5
        const val MAX_FONT_SIZE = 3.0
        const val MIN_LINE_HEIGHT = 1.0
        const val MAX_LINE_HEIGHT = 2.5
        private const val DEFAULT_LINE_HEIGHT = 1.5
        private const val DEFAULT_BRIGHTNESS = 0.5f
        private const val MAX_FONT_NAME = 128
    }
}
