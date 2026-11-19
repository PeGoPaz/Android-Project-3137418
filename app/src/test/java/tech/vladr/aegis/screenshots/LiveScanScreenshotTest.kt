package tech.vladr.aegis.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.vladr.aegis.ble.BleDevice
import tech.vladr.aegis.ble.Tracker
import tech.vladr.aegis.ble.TrackerKind
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
import tech.vladr.aegis.ui.scan.ScanScreen
import tech.vladr.aegis.ui.scan.ScanViewModel

/**
 * The Live Scan screen after a scan, drawn on the JVM by Robolectric and
 * captured by Roborazzi. The networks and devices are made up - no real
 * addresses - so the image can go in the README.
 *
 * ./gradlew recordRoborazziDebug saves the images; verifyRoborazziDebug
 * fails when a screen no longer looks like its saved image.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class LiveScanScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun locale() = fixLocale()

    private val networks = listOf(
        WifiNetwork("Library_Guest", "a4:5e:60:00:00:01", Security.WPA2, rssi = -48, frequencyMhz = 5180),
        WifiNetwork("Airport_Free_WiFi", "a4:5e:60:00:00:02", Security.OPEN, rssi = -61, frequencyMhz = 2412),
        WifiNetwork("Home", "a4:5e:60:00:00:03", Security.WPA3, rssi = -66, frequencyMhz = 5500),
        WifiNetwork("Pixel hotspot", "da:5e:60:00:00:04", Security.WPA2, rssi = -72, frequencyMhz = 2437),
        WifiNetwork("Old router", "a4:5e:60:00:00:05", Security.WEP, rssi = -84, frequencyMhz = 2462),
    )
    private val bluetooth = listOf(
        BleDevice("de:ad:be:ef:00:01", name = "", rssi = -67, tracker = Tracker(TrackerKind.AIRTAG, separated = true)),
        BleDevice("de:ad:be:ef:00:02", name = "Galaxy Watch", rssi = -58, tracker = null),
        BleDevice("de:ad:be:ef:00:03", name = "", rssi = -80, tracker = Tracker(TrackerKind.TILE)),
    )

    private class Known(devices: List<Device>) : ScanStore {
        private val known = MutableStateFlow(devices)
        override fun devices(): Flow<List<Device>> = known
        override suspend fun saveScan(networks: List<WifiNetwork>, fix: Fix?, time: Long, alerts: Boolean) = emptyList<Device>()
        override suspend fun saveBluetooth(devices: List<BleDevice>, fix: Fix?, time: Long) = emptyList<Device>()
    }

    @Test
    fun afterAScan() {
        val home = Device(id = 3, address = "a4:5e:60:00:00:03", name = "Home", kind = DeviceKind.WIFI_AP, status = DeviceStatus.HOME, firstSeen = 0, lastSeen = 0)
        val viewModel = ScanViewModel(
            scanner = object : NetworkScanner { override suspend fun scan() = ScanOutcome(networks, fresh = true) },
            locator = { Fix(53.3438, -6.2546, 9f) },
            repository = Known(listOf(home)),
            settings = object : SettingsSource { override val settings = flowOf(Settings()) },
            clock = { EVENING },
            bluetooth = { bluetooth },
        )
        compose.setContent { Screen { ScanScreen(viewModel = viewModel) } }
        compose.runOnIdle { viewModel.scan() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$SCREENSHOTS/live-scan.png")
    }
}
