package com.kmab.deft.feature.organization

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.library.organization.OrganizationRepository
import com.kmab.deft.library.organization.OrganizationSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

internal data class OrganizationUiState(
    val snapshot: OrganizationSnapshot? = null,
    val preferences: LibrarySettings = LibrarySettings(),
    val loadFailed: Boolean = false,
)

@HiltViewModel
internal class OrganizationViewModel
    @Inject
    constructor(
        private val repository: OrganizationRepository,
        private val settings: LibrarySettingsRepository,
        val covers: CategoryCoverStore,
    ) : ViewModel() {
        val state =
            combine(repository.observe(), settings.settings) { snapshot, prefs ->
                OrganizationUiState(snapshot, prefs)
            }.catch { emit(OrganizationUiState(loadFailed = true)) }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(ORGANIZATION_STOP_TIMEOUT),
                    OrganizationUiState(),
                )
        val busy = MutableStateFlow(false)
        val error = MutableStateFlow<String?>(null)
        val sheetActive = MutableStateFlow(false)
        val categorySelectionCompletion = MutableStateFlow(0L)

        fun completeCategorySelection() {
            categorySelectionCompletion.value += 1
        }

        fun edit(
            onDone: () -> Unit = {},
            action: suspend OrganizationRepository.() -> Unit,
        ) {
            write {
                repository.action()
                onDone()
            }
        }

        fun preference(action: suspend LibrarySettingsRepository.() -> Unit) =
            write(exclusive = false) { settings.action() }

        private fun write(
            exclusive: Boolean = true,
            action: suspend () -> Unit,
        ) {
            if (exclusive && busy.value) return
            if (exclusive) busy.value = true
            viewModelScope.launch {
                try {
                    action()
                } catch (invalid: IllegalArgumentException) {
                    error.value = invalid.message ?: "Check the entered values and try again."
                } catch (_: IOException) {
                    error.value = "Couldn't save the change. Try again."
                } catch (_: SecurityException) {
                    error.value = "Couldn't access the selected image. Choose it again."
                } catch (_: SQLiteException) {
                    error.value = "Couldn't save the change. Try again."
                } finally {
                    if (exclusive) busy.value = false
                }
            }
        }
    }

private const val ORGANIZATION_STOP_TIMEOUT = 5_000L
