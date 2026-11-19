package tech.vladr.aegis.screenshots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.vladr.aegis.ble.BleDevice
import tech.vladr.aegis.ble.Tracker
import tech.vladr.aegis.ble.TrackerKind
import tech.vladr.aegis.data.AegisDatabase
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.sensors.Fix
import tech.vladr.aegis.ui.alerts.AlertsScreen
import tech.vladr.aegis.ui.alerts.AlertsViewModel
import tech.vladr.aegis.ui.detail.DeviceDetailScreen
import tech.vladr.aegis.ui.detail.DeviceDetailViewModel

/**
 * An AirTag that follows you on a walk through Dublin city centre, seen
 * through the real repository and database - in memory, under Robolectric -
 * and the screens it ends up on: the alert it raises and its own page. The
 * places are public ones and the addresses made up.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class TrackerScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AegisDatabase
    private lateinit var repository: AegisRepository
    private var airtagId = 0L

    private val minute = 60_000L
    private val airtag = BleDevice("de:ad:be:ef:00:01", name = "", rssi = -67, tracker = Tracker(TrackerKind.AIRTAG, separated = true))

    @Before
    fun walk() {
        fixLocale()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AegisDatabase::class.java).build()
        repository = AegisRepository(db)
        runBlocking {
            // An open network passed first, then the AirTag at Trinity College, St Stephen's Green and Dublin Castle.
            repository.saveScan(
                listOf(WifiNetwork("Airport_Free_WiFi", "a4:5e:60:00:00:02", Security.OPEN, rssi = -61, frequencyMhz = 2412)),
                Fix(53.3498, -6.2603, 11f),
                time = EVENING - 90 * minute,
            )
            repository.saveBluetooth(listOf(airtag.copy(rssi = -71)), Fix(53.3445, -6.2590, 12f), time = EVENING - 40 * minute)
            repository.saveBluetooth(listOf(airtag.copy(rssi = -64)), Fix(53.3390, -6.2600, 9f), time = EVENING - 22 * minute)
            airtagId = repository.saveBluetooth(listOf(airtag), Fix(53.3429, -6.2674, 10f), time = EVENING).single().id
        }
    }

    @After
    fun close() = db.close()

    @Test
    fun theAlertItRaises() {
        val viewModel = AlertsViewModel(repository)
        compose.setContent { Screen { AlertsScreen(viewModel = viewModel) } }
        compose.waitUntilAtLeastOneExists(hasText("FOLLOWING"), timeoutMillis = 5_000)
        compose.onRoot().captureRoboImage("$SCREENSHOTS/alerts.png")
    }

    @Test
    fun itsOwnPage() {
        val viewModel = DeviceDetailViewModel(airtagId, repository)
        compose.setContent { Screen { DeviceDetailScreen(viewModel = viewModel, onBack = {}) } }
        compose.waitUntilAtLeastOneExists(hasText("History"), timeoutMillis = 5_000)
        compose.onRoot().captureRoboImage("$SCREENSHOTS/tracker-detail.png")
    }
}
