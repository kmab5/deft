package com.kmab.deft.feature.reader

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderSvgStylesheetTest {
    @Test fun splitStylesheetTokensCannotHideReferencesOrImports() {
        val styles =
            listOf(
                ".loop{fill:u<![CDATA[rl(#p)]]>}",
                ".loop{fill:&#117;rl(#p)}",
                ".loop{fill:u&#114;l(#p)}",
                ".loop{fill:u&#120;rl(#p)}",
                "@im<![CDATA[port]]> 'https://example.test/a.css';",
            )
        styles.forEach { css -> assertNull(safeReaderSvg(svg("<style>$css</style>"))) }
    }

    @Test fun referenceAttributesAreCheckedAfterXmlEntityDecoding() {
        assertNull(safeReaderSvg(svg("<image href=\"&#104;ttps://example.test/a.png\"/>")))
        assertNull(safeReaderSvg(svg("<rect style=\"fill:u&#114;l(https://example.test/a.svg)\"/>")))
    }

    @Test fun ordinaryAndCdataStylesheetColorsStillPaint() {
        listOf("#ff00ff", "<![CDATA[#]]>ff00ff").forEach { color ->
            val image =
                requireNotNull(
                    decodeReaderImage(
                        svg(
                            "<style>.color{fill:$color}</style>" +
                                "<rect class=\"color\" width=\"400\" height=\"600\"/>",
                        ),
                    ),
                )
            assertEquals(color, Color.MAGENTA, image.getPixel(image.width / 2, image.height / 2))
        }
    }

    @Test fun numericEntityColorIsSafeWithoutPromisingRendererSupport() {
        // AndroidSVG 1.4's pull-parser path ignores ENTITY_REF tokens. Our guard still decodes
        // them so a future parser change or SAX fallback cannot bypass reference validation.
        assertNotNull(safeReaderSvg(svg("<style>.color{fill:&#35;ff00ff}</style>")))
    }

    private fun svg(body: String) =
        ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"600\">$body</svg>").toByteArray()
}
