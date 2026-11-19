package com.kmab.deft.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.library.scanner.LibraryFolderRepository
import com.kmab.deft.library.scanner.ScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class LibrarySettingsViewModel
    @Inject
    constructor(
        val folders: LibraryFolderRepository,
        val settings: LibrarySettingsRepository,
        private val scheduler: ScanScheduler,
        observer: com.kmab.deft.library.scanner.ScanStatusObserver,
    ) : ViewModel() {
        val scanStatus = observer.status
        val error = MutableStateFlow<String?>(null)

        fun dismissError() {
            error.value = null
        }

        fun add(uri: Uri) = perform { folders.add(uri) }

        fun remove(root: String) = perform { folders.remove(root) }

        fun exclude(
            root: String,
            name: String,
            uri: Uri,
        ) = perform {
            try {
                folders.exclude(root, uri)
            } catch (_: IllegalArgumentException) {
                error.value = "Pick a folder inside $name"
            }
        }

        fun unexclude(
            root: String,
            id: String,
        ) = perform { folders.unexclude(root, id) }

        fun format(
            format: BookFormat,
            enabled: Boolean,
        ) = perform {
            settings.setFormatEnabled(format, enabled)
            scheduler.scanNow()
        }

        fun advancedSearch(value: Boolean) = perform { settings.setAdvancedSearch(value) }

        fun scanNow() = scheduler.scanNow()

        private fun perform(action: suspend () -> Unit) =
            viewModelScope.launch {
                try {
                    action()
                } catch (failure: IOException) {
                    reportFailure(failure)
                } catch (failure: SecurityException) {
                    reportFailure(failure)
                } catch (failure: IllegalArgumentException) {
                    reportFailure(failure)
                } catch (failure: UnsupportedOperationException) {
                    reportFailure(failure)
                }
            }

        private fun reportFailure(failure: Exception) {
            error.value = failure.message ?: "Couldn't update library. Try again."
        }
    }
