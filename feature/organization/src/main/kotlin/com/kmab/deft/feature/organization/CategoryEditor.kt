package com.kmab.deft.feature.organization

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
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
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.library.organization.CategorySummary

@Composable
internal fun CategoryEditor(
    current: CategorySummary?,
    types: List<CategoryTypeEntity>,
    model: OrganizationViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit = onClose,
    initialBooks: Set<Long>? = null,
) {
    val category = current?.category
    var name by rememberSaveable(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var description by rememberSaveable(category?.id) { mutableStateOf(category?.description.orEmpty()) }
    var checked by rememberSaveable(category?.id) { mutableStateOf(ArrayList(current?.types.orEmpty().map { it.id })) }
    var cover by rememberSaveable(category?.id) { mutableStateOf<Uri?>(null) }
    var removeCover by rememberSaveable(category?.id) { mutableStateOf(false) }
    val busy by model.busy.collectAsStateWithLifecycle()
    val pickCover =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                cover = it
                removeCover = false
            }
        }
    OrganizationSheet(if (category == null) "new category" else "edit category", onClose, model) {
        OutlinedTextField(
            name,
            { name = it },
            Modifier.fillMaxWidth(),
            label = { Text("name") },
            singleLine = true,
            enabled = !busy,
        )
        OutlinedTextField(
            description,
            { description = it },
            Modifier.fillMaxWidth(),
            label = { Text("description") },
            minLines = 2,
            enabled = !busy,
        )
        Text("types", style = DeftTypography.title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                FilterChip(type.id in checked, {
                    checked = ArrayList(if (type.id in checked) checked - type.id else checked + type.id)
                }, label = { Text(type.name) }, enabled = !busy)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeftButton(
                if (cover == null) "choose cover" else "change selected cover",
                { pickCover.launch("image/*") },
                style = DeftButtonStyle.Secondary,
                enabled = !busy,
            )
            if (cover != null || (!removeCover && category?.coverPath != null)) {
                DeftButton(
                    "use collage",
                    {
                        cover = null
                        removeCover = true
                    },
                    style = DeftButtonStyle.Text,
                    enabled = !busy,
                )
            }
        }
        val draft = CategoryDraft(name, description, checked.toSet(), cover, removeCover)
        DeftButton(
            "save category",
            { model.edit(onSaved) { persistCategory(draft, category, initialBooks, model.covers) } },
            enabled = !busy && draft.isValid,
        )
    }
}
