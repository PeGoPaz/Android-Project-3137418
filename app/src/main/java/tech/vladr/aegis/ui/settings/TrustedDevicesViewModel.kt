package tech.vladr.aegis.ui.settings

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
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceStatus

class TrustedDevicesViewModel(private val repository: AegisRepository, private val clock: () -> Long = System::currentTimeMillis) : ViewModel() {
    /** null until loaded. */
    val devices: StateFlow<List<Device>?> = repository.ownDevices()
        .map<List<Device>, List<Device>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(address: String, name: String, status: DeviceStatus) =
        viewModelScope.launch { repository.addOwnDevice(normaliseMac(address), name, status, clock()) }

    fun rename(id: Long, name: String) = viewModelScope.launch { repository.rename(id, name) }

    /** No longer yours: back to unknown, kept with its history. */
    fun forget(id: Long) = viewModelScope.launch { repository.setStatus(id, DeviceStatus.UNKNOWN) }

    fun delete(id: Long) = viewModelScope.launch { repository.deleteDevice(id) }

    companion object {
        val Factory = viewModelFactory {
            initializer { TrustedDevicesViewModel((this[APPLICATION_KEY] as AegisApplication).repository) }
        }
    }
}
