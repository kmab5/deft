package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography

@Composable
internal fun CategoryTypes(
    types: List<CategoryTypeEntity>,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    val busy by model.busy.collectAsStateWithLifecycle()
    OrganizationSheet("category types", close, model) {
        types.forEach { type ->
            if (type.builtIn) {
                Text("${type.name} · built in", style = DeftTypography.title)
            } else {
                OrganizationRow(type.name, "edit type", !busy) {
                    selectedId = type.id
                    name = type.name
                }
            }
        }
        OutlinedTextField(
            name,
            { name = it },
            Modifier.fillMaxWidth(),
            label = { Text(if (selectedId == null) "new type name" else "type name") },
            singleLine = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeftButton(if (selectedId == null) "create type" else "save type", {
                model.edit({
                    name = ""
                    selectedId = null
                }) { saveType(selectedId, name) }
            }, enabled = !busy && name.isNotBlank() && name.length <= 240)
            if (selectedId != null) {
                DeftButton("delete type", { confirming = true }, style = DeftButtonStyle.Secondary, enabled = !busy)
                DeftButton("new type", {
                    selectedId = null
                    name = ""
                }, style = DeftButtonStyle.Text, enabled = !busy)
            }
        }
    }
    if (confirming) {
        DeftDialog("delete type?", "Categories and books will be kept.", "delete type", {
            confirming = false
            val id = selectedId
            if (id != null) {
                model.edit({
                    selectedId = null
                    name = ""
                }) { deleteType(id) }
            }
        }, { confirming = false })
    }
}
