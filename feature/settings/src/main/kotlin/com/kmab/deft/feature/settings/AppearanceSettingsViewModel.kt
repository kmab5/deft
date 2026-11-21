package com.kmab.deft.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.datastore.AppearanceSettingsRepository
import com.kmab.deft.core.datastore.TrackingSettingsRepository
import com.kmab.deft.core.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel
    @Inject
    constructor(
        private val appearanceRepository: AppearanceSettingsRepository,
        private val trackingRepository: TrackingSettingsRepository,
    ) : ViewModel() {
        val error = MutableStateFlow<String?>(null)
        val theme = appearanceRepository.theme
        val celebrations =
            trackingRepository.preferences
                .map { it.celebrations }
                .catch { failure ->
                    if (failure !is IOException) throw failure
                    error.value = "Couldn't load the celebrations preference. Go back and try again."
                    emit(true)
                }.distinctUntilChanged()

        fun setTheme(theme: AppTheme) {
            save { appearanceRepository.setTheme(theme) }
        }

        fun setCelebrations(celebrations: Boolean) {
            save { trackingRepository.update { it.copy(celebrations = celebrations) } }
        }

        private fun save(write: suspend () -> Unit) {
            viewModelScope.launch {
                try {
                    write()
                } catch (_: IOException) {
                    error.value = "Couldn't save preferences. Try again."
                }
            }
        }
    }
