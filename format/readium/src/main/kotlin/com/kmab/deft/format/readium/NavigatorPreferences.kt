@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.format.readium

import android.graphics.Color.parseColor
import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.RsProperties
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.util.Url
import org.readium.r2.navigator.epub.css.Color as CssColor

fun ReaderPreferences.toNavigatorPreferences(theme: ReaderTheme): EpubPreferences =
    EpubPreferences(
        backgroundColor = Color(parseColor(theme.background)),
        textColor = Color(parseColor(theme.text)),
        theme = if (android.graphics.Color.luminance(parseColor(theme.background)) < 0.3f) Theme.DARK else Theme.LIGHT,
        // Readium's fontOverride flag is false when this is null. Advanced typography remains enabled.
        fontFamily = fontFamily.takeUnless { it == "original" }?.let(::FontFamily),
        textNormalization = false,
        fontSize = fontSize,
        lineHeight = lineHeight,
        pageMargins =
            when (margins) {
                ReaderMargins.SMALL -> 0.5
                ReaderMargins.MEDIUM -> 1.0
                ReaderMargins.LARGE -> 1.5
            },
        textAlign = if (alignment == ReaderAlignment.JUSTIFY) TextAlign.JUSTIFY else TextAlign.START,
        hyphens = hyphenation,
        scroll = scroll,
        publisherStyles = false,
    )

fun navigatorConfiguration(
    fonts: List<ReaderFont>,
    theme: ReaderTheme,
): EpubNavigatorFragment.Configuration =
    EpubNavigatorFragment.Configuration {
        servedAssets = listOf("reader-fonts/.*")
        shouldApplyInsetsPadding = false
        readiumCssRsProperties =
            RsProperties(
                linkColor = CssColor.Int(parseColor(theme.accent)),
                visitedColor = CssColor.Int(parseColor(theme.accent)),
            )
        // Publisher-original has no custom faces; Readium rejects empty family declarations.
        fonts.filter { it.faces.isNotEmpty() }.forEach { font ->
            addFontFamilyDeclaration(FontFamily(font.id)) {
                font.faces.forEachIndexed { index, face ->
                    addFontFace {
                        face.assetPath?.let { addSource(it) }
                        face.privateFilePath?.let {
                            addSource(
                                requireNotNull(Url("https://readium_package/${importedFontPath(font.id, index)}")),
                            )
                        }
                        setFontStyle(if (face.italic) FontStyle.ITALIC else FontStyle.NORMAL)
                        setFontWeight(face.weight..face.weight)
                    }
                }
            }
        }
    }
