package tech.vladr.aegis.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Sighting

data class DeviceDetailUiState(
    val device: Device? = null,
    val sightings: List<Sighting> = emptyList(),
    /** False until the database has answered; then a null device means it was deleted. */
    val loaded: Boolean = false,
) {
    val strongest: Int? get() = sightings.maxOfOrNull { it.rssi }
}

/** One device and its history, and the changes you can make to it. */
class DeviceDetailViewModel(private val deviceId: Long, private val repository: AegisRepository) : ViewModel() {
    val state: StateFlow<DeviceDetailUiState> =
        combine(repository.device(deviceId), repository.sightings(deviceId)) { device, sightings ->
            DeviceDetailUiState(device, sightings, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceDetailUiState())

    fun setStatus(status: DeviceStatus) = viewModelScope.launch { repository.setStatus(deviceId, status) }

    fun rename(name: String) = viewModelScope.launch { repository.rename(deviceId, name) }

    fun clearHistory() = viewModelScope.launch { repository.clearHistory(deviceId) }

    fun delete(then: () -> Unit) = viewModelScope.launch {
        repository.deleteDevice(deviceId)
        then()
    }

    companion object {
        fun factory(deviceId: Long) = viewModelFactory {
            initializer { DeviceDetailViewModel(deviceId, (this[APPLICATION_KEY] as AegisApplication).repository) }
        }
    }
}
