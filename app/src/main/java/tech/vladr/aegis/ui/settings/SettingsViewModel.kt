package tech.vladr.aegis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.data.Settings
import tech.vladr.aegis.data.SettingsStore

class SettingsViewModel(private val store: SettingsStore, private val repository: AegisRepository) : ViewModel() {
    val settings: StateFlow<Settings> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setShowHiddenNetworks(value: Boolean) = viewModelScope.launch { store.setShowHiddenNetworks(value) }
    fun setSaveWithoutLocation(value: Boolean) = viewModelScope.launch { store.setSaveWithoutLocation(value) }
    fun setUnsafeNetworkAlerts(value: Boolean) = viewModelScope.launch { store.setUnsafeNetworkAlerts(value) }
    fun deleteEverything() = viewModelScope.launch { repository.deleteEverything() }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AegisApplication
                SettingsViewModel(app.settings, app.repository)
            }
        }
    }
}
