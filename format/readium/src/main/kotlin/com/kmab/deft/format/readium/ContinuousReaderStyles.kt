package com.kmab.deft.format.readium

import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme

/** Reader overrides shared by isolated chapter frames; publisher selectors never cross chapters. */
fun continuousReaderStyles(
    preferences: ReaderPreferences,
    theme: ReaderTheme,
    fonts: List<ReaderFont>,
): String {
    val settings = preferences.normalized()
    val family = fonts.firstOrNull { it.id == settings.fontFamily && it.faces.isNotEmpty() }
    val padding =
        when (settings.margins) {
            ReaderMargins.SMALL -> SMALL_MARGIN_PX
            ReaderMargins.MEDIUM -> MEDIUM_MARGIN_PX
            ReaderMargins.LARGE -> LARGE_MARGIN_PX
        }
    val alignment =
        when (settings.alignment) {
            ReaderAlignment.ORIGINAL -> ""
            ReaderAlignment.START -> "text-align:start!important;"
            ReaderAlignment.JUSTIFY -> "text-align:justify!important;"
        }
    val fontDeclaration = if (family == null) "" else "font-family:'DeftReaderFont'!important;"
    return buildString {
        family?.faces?.forEachIndexed { index, face ->
            val url =
                if (face.assetPath != null || face.privateFilePath != null) {
                    "https://readium_package/${importedFontPath(family.id, index)}"
                } else {
                    null
                }
            if (url != null) {
                append("@font-face{font-family:'DeftReaderFont';src:url(")
                append(org.json.JSONObject.quote(url))
                append(");font-weight:${face.weight};font-style:${if (face.italic) "italic" else "normal"};}")
            }
        }
        append(
            "html{font-size:${settings.fontSize * PERCENT_SCALE}%;background:${safeColor(theme.background)}!important;",
        )
        append("color:${safeColor(theme.text)}!important;text-size-adjust:none;-webkit-text-size-adjust:none;}")
        append("body{margin:0!important;padding:20px ${padding}px!important;box-sizing:border-box!important;")
        append("max-width:none!important;width:auto!important;background:transparent!important;}")
        append("body,p,li,blockquote,dd,dt{font-size:1rem!important;")
        append("line-height:${settings.lineHeight}!important;$alignment$fontDeclaration}")
        append("h1,h2,h3,h4,h5,h6,span{$fontDeclaration}")
        append("body *{color:inherit!important;background-color:transparent!important;}")
        append("p,li,dd{hyphens:${if (settings.hyphenation) "auto" else "none"}!important;overflow-wrap:break-word;}")
        append("img,svg,image,video{max-width:100%!important;height:auto;object-fit:contain;}")
        append("pre,table{max-width:100%;overflow-wrap:anywhere;}")
        append("a,a *{color:${safeColor(theme.accent)}!important;}")
    }
}

private fun safeColor(color: String): String = color.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) } ?: "#202020"

private const val SMALL_MARGIN_PX = 12
private const val MEDIUM_MARGIN_PX = 24
private const val LARGE_MARGIN_PX = 36
private const val PERCENT_SCALE = 100
