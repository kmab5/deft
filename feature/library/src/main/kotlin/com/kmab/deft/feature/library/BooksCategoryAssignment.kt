package com.kmab.deft.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.feature.organization.CategoryMembershipPicker

/** Saves the pending selection with the destination, including while Android's image picker is open. */
@Composable
internal fun BooksCategoryAssignment(
    picker: @Composable (Set<Long>, () -> Unit, () -> Unit) -> Unit = { ids, dismiss, added ->
        CategoryMembershipPicker(ids, dismiss, added)
    },
    content: @Composable ((Set<Long>) -> Unit, Int) -> Unit,
) {
    var pending by rememberSaveable { mutableStateOf<ArrayList<Long>?>(null) }
    var completed by rememberSaveable { mutableStateOf(0) }
    val notices = LocalDeftNoticeState.current
    content({ pending = ArrayList(it) }, completed)
    pending?.let { ids ->
        picker(ids.toSet(), { pending = null }, {
            completed++
            pending = null
            notices?.show("Added to category", kind = DeftNoticeKind.Success)
        })
    }
}
