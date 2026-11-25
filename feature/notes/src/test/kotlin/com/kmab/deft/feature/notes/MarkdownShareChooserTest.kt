package com.kmab.deft.feature.notes

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarkdownShareChooserTest {
    @Test fun chooserAndTargetGrantReadAccessToTheSingleMarkdownFile() {
        val uri = Uri.parse("content://com.kmab.deft.notes-share/notes/export/deft-notes.md")
        val chooser = markdownShareChooser(uri)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val target = requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
        assertEquals(Intent.ACTION_SEND, target.action)
        assertEquals("text/markdown", target.type)
        assertEquals(uri, target.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        val grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        listOf(chooser, target).forEach { intent ->
            assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and grantFlags)
            assertEquals(1, intent.clipData?.itemCount)
            assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
        }
    }
}
