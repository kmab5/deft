package com.kmab.deft.feature.readersettings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.datastore.ReaderSettingsRepository
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ReaderSettingsViewModel
    @Inject
    constructor(
        private val settings: ReaderSettingsRepository,
        private val fontRepository: ReaderFontRepository,
    ) : ViewModel() {
        val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, ReaderPreferences())
        val themes = settings.customThemes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        val fonts = fontRepository.fonts
        private val messages = Channel<String>(Channel.BUFFERED)
        val errors = messages.receiveAsFlow()

        fun update(transform: (ReaderPreferences) -> ReaderPreferences) = perform { settings.update(transform) }

        fun removeFont(id: String) = perform { fontRepository.removeFont(id) }

        fun importFont(uri: Uri) =
            perform {
                val font = fontRepository.importFont(uri)
                settings.update { it.copy(fontFamily = font.id) }
            }

        fun deleteTheme(id: String) = perform { settings.deleteTheme(id) }

        suspend fun saveTheme(theme: ReaderTheme) {
            settings.upsertTheme(theme)
        }

        private fun perform(action: suspend () -> Unit) {
            viewModelScope.launch {
                try {
                    action()
                } catch (_: IOException) {
                    messages.send("The change could not be saved. Check available storage and try again.")
                } catch (error: IllegalArgumentException) {
                    messages.send(error.message ?: "This setting could not be applied.")
                } catch (_: SecurityException) {
                    messages.send("This font is no longer available. Choose the file again.")
                }
            }
        }
    }
