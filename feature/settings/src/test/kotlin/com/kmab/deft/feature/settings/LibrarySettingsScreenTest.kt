package com.kmab.deft.feature.settings

import com.kmab.deft.core.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySettingsScreenTest {
    @Test fun allNineFormatsHaveDistinctLabelsIncludingAliases() {
        val labels = BookFormat.entries.map(::formatLabel)
        assertEquals(9, labels.distinct().size)
        assertEquals("MOBI/PRC", formatLabel(BookFormat.MOBI))
        assertEquals("DjVu", formatLabel(BookFormat.DJVU))
    }
}
