package tech.vladr.aegis.ui.scan

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.ScanStore
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.data.Settings
import tech.vladr.aegis.data.SettingsSource
import tech.vladr.aegis.scan.NetworkScanner
import tech.vladr.aegis.scan.ScanOutcome
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.sensors.Fix

/** The Live Scan logic with a fake Wi-Fi, a fixed location and a store in memory: no phone needed. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val cafe = WifiNetwork(ssid = "Cafe", bssid = "aa:bb:cc:dd:ee:01", security = Security.WPA2, rssi = -60, frequencyMhz = 2412)
    private val hidden = WifiNetwork(ssid = "", bssid = "aa:bb:cc:dd:ee:02", security = Security.WPA2, rssi = -70, frequencyMhz = 5180)
    private val here = Fix(53.3498, -6.2603, 8f)

    /** Remembers every save; its devices are whatever a test puts in. */
    private class MemoryStore : ScanStore {
        val saves = mutableListOf<Triple<List<WifiNetwork>, Fix?, Boolean>>()
        val known = MutableStateFlow<List<Device>>(emptyList())
        override fun devices(): Flow<List<Device>> = known
        override suspend fun saveScan(networks: List<WifiNetwork>, fix: Fix?, time: Long, alerts: Boolean): List<Device> {
            saves += Triple(networks, fix, alerts)
            return emptyList()
        }
    }

    private class FixedSettings(value: Settings) : SettingsSource {
        override val settings = MutableStateFlow(value)
    }

    private val store = MemoryStore()

    private fun viewModel(
        outcome: ScanOutcome = ScanOutcome(listOf(cafe, hidden), fresh = true),
        fix: Fix? = here,
        settings: Settings = Settings(),
        scanner: NetworkScanner = object : NetworkScanner { override suspend fun scan() = outcome },
    ) = ScanViewModel(scanner, { fix }, store, FixedSettings(settings), clock = { 1_000L })

    @Before
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun reset() = Dispatchers.resetMain()

    @Test
    fun aFreshScanIsSavedWithTheLocation() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.scan()
        assertEquals(listOf(Triple(listOf(cafe, hidden), here, true)), store.saves)
        val state = vm.state.value
        assertEquals(listOf(cafe, hidden), state.networks)
        assertEquals(1_000L, state.lastScanAt)
        assertEquals(here, state.fix)
        assertFalse(state.scanning)
        assertNull(state.notice)
    }

    @Test
    fun oldResultsAreShownButNotSavedAgain() = runTest {
        val vm = viewModel(outcome = ScanOutcome(listOf(cafe), fresh = false))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.scan()
        assertTrue(store.saves.isEmpty())
        assertEquals(listOf(cafe), vm.state.value.networks)
        assertEquals(ScanViewModel.THROTTLED, vm.state.value.notice)
    }

    @Test
    fun withoutALocation_theSettingDecidesWhetherItIsSaved() = runTest {
        val strict = viewModel(fix = null, settings = Settings(saveWithoutLocation = false))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { strict.state.collect {} }
        strict.scan()
        assertTrue(store.saves.isEmpty())
        assertEquals(ScanViewModel.NOT_SAVED, strict.state.value.notice)

        val lenient = viewModel(fix = null)
        lenient.scan()
        assertEquals(listOf<Fix?>(null), store.saves.map { it.second })
    }

    @Test
    fun alertsCanBeTurnedOff() = runTest {
        viewModel(settings = Settings(unsafeNetworkAlerts = false)).scan()
        assertEquals(listOf(false), store.saves.map { it.third })
    }

    @Test
    fun hiddenNetworksAreLeftOutWhenSet() = runTest {
        val vm = viewModel(settings = Settings(showHiddenNetworks = false))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.scan()
        assertEquals(listOf(cafe), vm.state.value.networks)
        // Hidden networks are still saved: the setting only changes what is listed.
        assertEquals(listOf(cafe, hidden), store.saves.single().first)
    }

    @Test
    fun whatTheDatabaseKnowsIsShownByAddress() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val mine = Device(id = 3, address = cafe.bssid, name = "Cafe", kind = DeviceKind.WIFI_AP, status = DeviceStatus.HOME, firstSeen = 0, lastSeen = 0)
        store.known.value = listOf(mine)
        assertEquals(DeviceStatus.HOME, vm.state.value.known[cafe.bssid]?.status)
    }

    @Test
    fun aSecondPressWhileScanningIsIgnored() = runTest {
        val release = CompletableDeferred<Unit>()
        var scans = 0
        val slow = object : NetworkScanner {
            override suspend fun scan(): ScanOutcome {
                scans += 1
                release.await()
                return ScanOutcome(listOf(cafe), fresh = true)
            }
        }
        val vm = viewModel(scanner = slow)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.scan()
        assertTrue(vm.state.value.scanning)
        vm.scan()
        release.complete(Unit)
        assertEquals(1, scans)
        assertEquals(1, store.saves.size)
        assertFalse(vm.state.value.scanning)
    }
}
