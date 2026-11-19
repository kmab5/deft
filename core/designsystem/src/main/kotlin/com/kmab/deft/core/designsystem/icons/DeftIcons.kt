package com.kmab.deft.core.designsystem.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder

/** Established Material outlined vectors; directional actions mirror in RTL layouts. */
object DeftIcons {
    val Books get() = Icons.Outlined.AutoStories
    val Categories get() = Icons.Outlined.GridView
    val Authors get() = Icons.Outlined.PeopleOutline
    val Notes get() = Icons.AutoMirrored.Outlined.Notes
    val Stats get() = Icons.Outlined.BarChart
    val Search get() = Icons.Outlined.Search
    val Filter get() = Icons.Outlined.FilterList
    val Lock get() = Icons.Outlined.Lock
    val Heart get() = Icons.Outlined.FavoriteBorder
    val Star get() = Icons.Outlined.StarBorder
}
