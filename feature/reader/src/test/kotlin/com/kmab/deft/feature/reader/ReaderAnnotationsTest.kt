package com.kmab.deft.feature.reader

import android.content.Intent
import com.kmab.deft.core.database.entity.HighlightEntity
import com.kmab.deft.core.model.HighlightColor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderAnnotationsTest {
    private val locator =
        """
        {"href":"chapter.xhtml","type":"application/xhtml+xml",
        "locations":{"progression":0.3},"text":{"highlight":"quiet passage"}}
        """.trimIndent()

    @Test fun colorlessAndMalformedAnnotationsNeverCreateDecorations() {
        val colored = entry(HighlightColor.PURPLE)
        assertEquals("7", readerDecorations(listOf(colored)).single().id)
        assertTrue(readerDecorations(listOf(colored.copy(color = HighlightColor.NONE))).isEmpty())
        assertTrue(readerDecorations(listOf(colored.copy(locator = "broken"))).isEmpty())
        val continuous = continuousAnnotations(listOf(colored, colored.copy(id = 8, color = HighlightColor.NONE)))
        assertEquals(1, continuous.length())
        assertEquals("quiet passage", continuous.getJSONObject(0).getString("text"))
    }

    @Test fun captureKeepsRangeAndContextAcrossLocatorSerialization() {
        val original = parseLocator(locator)!!
        val capture =
            JSONObject(
                """
                {"text":"quiet passage","before":"A ","after":" ends","cssSelector":"#p2",
                "domRange":{"start":{"cssSelector":"#p2","textNodeIndex":0,"charOffset":2},
                "end":{"cssSelector":"#p2","textNodeIndex":0,"charOffset":15},"text":"quietpassage"}}
                """.trimIndent(),
            )
        val anchored = selectionLocator(original, capture)
        val restored = parseLocator(anchored.toJSON().toString())!!
        assertEquals("A ", restored.text.before)
        assertEquals("#p2", restored.locations.otherLocations["cssSelector"])
        assertTrue(restored.toJSON().getJSONObject("locations").has("domRange"))
        assertEquals("quietpassage", restored.forReadiumRange().text.highlight)
        assertEquals("quiet passage", restored.text.highlight)
    }

    @Test fun externalActionsCarryOnlyExplicitSelectedText() {
        val share = selectionIntent(ReaderSelectionAction.SHARE, "my passage")!!
        assertEquals(Intent.ACTION_SEND, share.action)
        assertEquals("text/plain", share.type)
        assertEquals("my passage", share.getStringExtra(Intent.EXTRA_TEXT))
        val define = selectionIntent(ReaderSelectionAction.DEFINE, "word")!!
        assertEquals(Intent.ACTION_PROCESS_TEXT, define.action)
        assertTrue(define.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))
        assertFalse(define.hasExtra(Intent.EXTRA_STREAM))
        assertNull(selectionIntent(ReaderSelectionAction.HIGHLIGHT, "word"))
    }

    private fun entry(color: HighlightColor) =
        HighlightEntity(
            id = 7,
            bookId = 1,
            locator = locator,
            text = "quiet passage",
            color = color,
            note = "remember",
            totalProgression = 0.3,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
}
