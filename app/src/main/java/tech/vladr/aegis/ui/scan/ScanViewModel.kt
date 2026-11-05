package tech.vladr.aegis.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.scan.WifiScanner
import tech.vladr.aegis.sensors.Fix
import tech.vladr.aegis.sensors.Locator

/** What the Live Scan screen shows. */
data class ScanUiState(
    val networks: List<WifiNetwork> = emptyList(),
    val scanning: Boolean = false,
    val lastScanAt: Long? = null,
    /** Where the phone was during the last scan, or null without a fix. */
    val fix: Fix? = null,
    /** Why the results may be old, when they are. */
    val notice: String? = null,
    /** What the database knows about each network, by BSSID: its status, a name you gave it. */
    val known: Map<String, Device> = emptyMap(),
)

/**
 * Runs scans for the Live Scan screen, saves them with the phone's location,
 * and keeps the latest results. A ViewModel outlives the screen when the
 * phone is turned, so a scan in progress is not lost.
 */
class ScanViewModel(
    private val scanner: WifiScanner,
    private val locator: Locator,
    private val repository: AegisRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val scan = MutableStateFlow(ScanUiState())

    val state: StateFlow<ScanUiState> = combine(scan, repository.devices()) { state, devices ->
        state.copy(known = devices.associateBy { it.address })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    fun scan() {
        if (scan.value.scanning) return
        scan.update { it.copy(scanning = true, notice = null) }
        viewModelScope.launch {
            coroutineScope {
                // The GPS fix and the scan take a few seconds each; they run side by side.
                val fix = async { locator.current() }
                val outcome = scanner.scan()
                val time = clock()
                val where = fix.await()
                // Old results handed back by Android were saved when they were new.
                if (outcome.fresh) repository.saveScan(outcome.networks, where, time)
                scan.update {
                    it.copy(
                        networks = outcome.networks,
                        scanning = false,
                        lastScanAt = time,
                        fix = where,
                        notice = if (outcome.fresh) null else THROTTLED,
                    )
                }
            }
        }
    }

    companion object {
        const val THROTTLED = "Android allows four scans every two minutes, so these are the latest results it has. They are not saved again."

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AegisApplication
                ScanViewModel(app.scanner, app.locator, app.repository)
            }
        }
    }
}
