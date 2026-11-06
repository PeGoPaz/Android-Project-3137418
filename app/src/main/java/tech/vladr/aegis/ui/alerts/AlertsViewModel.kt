package tech.vladr.aegis.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.data.AlertWithDevice

/** null until the database has answered, so the screen does not flash "No alerts" first. */
class AlertsViewModel(private val repository: AegisRepository) : ViewModel() {
    val alerts: StateFlow<List<AlertWithDevice>?> = repository.activeAlerts()
        .map<List<AlertWithDevice>, List<AlertWithDevice>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismiss(alertId: Long) = viewModelScope.launch { repository.dismissAlert(alertId) }

    companion object {
        val Factory = viewModelFactory {
            initializer { AlertsViewModel((this[APPLICATION_KEY] as AegisApplication).repository) }
        }
    }
}
