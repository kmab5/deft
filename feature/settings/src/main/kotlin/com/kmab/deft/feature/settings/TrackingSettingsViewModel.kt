package com.kmab.deft.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.datastore.TrackingSettingsRepository
import com.kmab.deft.core.model.TrackingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingSettingsViewModel
    @Inject
    constructor(
        private val repository: TrackingSettingsRepository,
    ) : ViewModel() {
        val error = MutableStateFlow<String?>(null)
        val preferences =
            repository.preferences
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    error.value = "Couldn't load tracking preferences. Go back and try again."
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SETTINGS_STOP_TIMEOUT), TrackingPreferences())

        fun update(transform: (TrackingPreferences) -> TrackingPreferences) {
            viewModelScope.launch {
                try {
                    repository.update(transform)
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (
                    _: java.io.IOException,
                ) {
                    error.value = "Couldn't save preferences. Try again."
                }
            }
        }
    }

private const val SETTINGS_STOP_TIMEOUT = 5_000L
