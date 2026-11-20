package com.kmab.deft.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.model.SearchOptions

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SearchOptionsControls(
    options: SearchOptions,
    onChange: (SearchOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDeftColors.current
    Surface(
        modifier.fillMaxWidth().testTag("search_options"),
        color = colors.surface2,
        shape = DeftShapes.card,
        border = BorderStroke(1.dp, colors.line),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}
