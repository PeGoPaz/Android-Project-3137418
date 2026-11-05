package tech.vladr.aegis.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.data.AegisRepository

data class MapUiState(val route: List<MapPoint> = emptyList(), val markers: List<DeviceMarker> = emptyList(), val loaded: Boolean = false)

/** The route and the markers, redrawn whenever a scan is saved or a status changes. */
class MapViewModel(repository: AegisRepository) : ViewModel() {
    val state: StateFlow<MapUiState> = combine(repository.devices(), repository.locatedSightings()) { devices, sightings ->
        MapUiState(route = route(sightings), markers = placeDevices(devices, sightings), loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { MapViewModel((this[APPLICATION_KEY] as AegisApplication).repository) }
        }
    }
}
