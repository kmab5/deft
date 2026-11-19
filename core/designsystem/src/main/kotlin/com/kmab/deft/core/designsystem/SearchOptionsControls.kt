package com.kmab.deft.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.model.SearchOptions

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SearchOptionsControls(
    options: SearchOptions,
    onChange: (SearchOptions) -> Unit,
) {
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                options.matchCase,
                { onChange(options.copy(matchCase = !options.matchCase)) },
                { Text("Match case") },
            )
            FilterChip(
                options.wholeWord,
                { onChange(options.copy(wholeWord = !options.wholeWord)) },
                { Text("Whole word") },
            )
            FilterChip(options.regex, { onChange(options.copy(regex = !options.regex)) }, { Text("Regex") })
        }
        if (options.regex) {
            Text(
                "Regex: try moon|sun or mo+n. No lookaround or backreferences. Maximum 256 characters.",
                style = DeftTypography.label,
            )
        }
    }
}
