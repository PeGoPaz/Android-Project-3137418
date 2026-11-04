package tech.vladr.aegis.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.scan.WifiScanner

/** What the Live Scan screen shows. */
data class ScanUiState(
    val networks: List<WifiNetwork> = emptyList(),
    val scanning: Boolean = false,
    val lastScanAt: Long? = null,
    /** Why the results may be old, when they are. */
    val notice: String? = null,
)

/**
 * Runs scans for the Live Scan screen and keeps the latest results. A
 * ViewModel outlives the screen when the phone is turned, so a scan in
 * progress is not lost.
 */
class ScanViewModel(private val scanner: WifiScanner, private val clock: () -> Long = System::currentTimeMillis) : ViewModel() {
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.scanning) return
        _state.update { it.copy(scanning = true, notice = null) }
        viewModelScope.launch {
            val outcome = scanner.scan()
            _state.update {
                it.copy(
                    networks = outcome.networks,
                    scanning = false,
                    lastScanAt = clock(),
                    notice = if (outcome.fresh) null else THROTTLED,
                )
            }
        }
    }

    companion object {
        const val THROTTLED = "Android allows four scans every two minutes, so these are the latest results it has."

        val Factory = viewModelFactory {
            initializer { ScanViewModel((this[APPLICATION_KEY] as AegisApplication).scanner) }
        }
    }
}
