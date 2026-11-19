package tech.vladr.aegis.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.vladr.aegis.ble.BleDevice
import tech.vladr.aegis.ble.Tracker
import tech.vladr.aegis.ble.TrackerKind
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.sensors.Fix

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AegisDatabase
    private lateinit var repository: AegisRepository

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AegisDatabase::class.java).build()
        repository = AegisRepository(db)
    }

    @After
    fun close() = db.close()

    private val cafe = WifiNetwork(ssid = "Cafe", bssid = "aa:bb:cc:dd:ee:01", security = Security.WPA2, rssi = -60, frequencyMhz = 2412)
    private val airport = WifiNetwork(ssid = "Airport_Free_WiFi", bssid = "aa:bb:cc:dd:ee:02", security = Security.OPEN, rssi = -70, frequencyMhz = 5180)

    @Test
    fun aNetworkSeenTwiceIsOneDeviceWithTwoSightings() = runTest {
        repository.saveScan(listOf(cafe), Fix(53.3498, -6.2603, 8f), time = 1_000)
        repository.saveScan(listOf(cafe.copy(rssi = -50)), Fix(53.3501, -6.2610, 12f), time = 2_000)

        val device = db.devices().findByAddress(cafe.bssid)!!
        assertEquals(1_000L, device.firstSeen)
        assertEquals(2_000L, device.lastSeen)
        val sightings = db.sightings().observeForDevice(device.id).first()
        assertEquals(listOf(-50, -60), sightings.map { it.rssi })
        assertEquals(53.3501, sightings.first().latitude!!, 1e-9)
    }

    @Test
    fun everyNetworkOfAScanIsSaved() = runTest {
        val saved = repository.saveScan(listOf(cafe, airport), Fix(53.35, -6.26, 5f), time = 1_000)
        assertEquals(listOf("Cafe", "Airport_Free_WiFi"), saved.map { it.name })
        assertEquals(2, repository.devices().first().size)
    }

    @Test
    fun aScanWithoutALocationIsSavedWithoutOne() = runTest {
        repository.saveScan(listOf(cafe), fix = null, time = 1_000)
        val sighting = db.sightings().observeForDevice(db.devices().findByAddress(cafe.bssid)!!.id).first().single()
        assertNull(sighting.latitude)
        assertEquals(emptyList<Sighting>(), db.sightings().observeLocated().first())
    }

    @Test
    fun aNetworkThatChangesItsNameKeepsItsStatus() = runTest {
        val id = repository.saveScan(listOf(cafe), fix = null, time = 1_000).single().id
        db.devices().setStatus(id, DeviceStatus.TRUSTED)
        repository.saveScan(listOf(cafe.copy(ssid = "Cafe 2")), fix = null, time = 2_000)
        val device = db.devices().findByAddress(cafe.bssid)!!
        assertEquals("Cafe 2", device.name)
        assertEquals(DeviceStatus.TRUSTED, device.status)
    }

    @Test
    fun aTrackerIsSavedAsABluetoothDeviceAndKeepsItsKind() = runTest {
        val tag = BleDevice(address = "da:a1:19:7c:30:02", name = "", rssi = -78, tracker = Tracker(TrackerKind.AIRTAG, separated = true))
        val saved = repository.saveBluetooth(listOf(tag), Fix(53.3445, -6.2590, 12f), time = 1_000).single()
        assertEquals(DeviceKind.BLE, saved.kind)
        assertEquals(TrackerKind.AIRTAG, saved.tracker)
        // A later advertisement without the Find My message does not forget what it is.
        repository.saveBluetooth(listOf(tag.copy(tracker = null, rssi = -70)), fix = null, time = 2_000)
        assertEquals(TrackerKind.AIRTAG, db.devices().findByAddress(tag.address)?.tracker)
        assertEquals(2, repository.sightings(saved.id).first().size)
    }
}

@RunWith(AndroidJUnit4::class)
class AlertsTest {
    private lateinit var db: AegisDatabase
    private lateinit var repository: AegisRepository

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AegisDatabase::class.java).build()
        repository = AegisRepository(db)
    }

    @After
    fun close() = db.close()

    private val airport = WifiNetwork(ssid = "Airport_Free_WiFi", bssid = "aa:bb:cc:dd:ee:02", security = Security.OPEN, rssi = -70, frequencyMhz = 5180)
    private val old = WifiNetwork(ssid = "Old router", bssid = "aa:bb:cc:dd:ee:03", security = Security.WEP, rssi = -75, frequencyMhz = 2437)
    private val cafe = WifiNetwork(ssid = "Cafe", bssid = "aa:bb:cc:dd:ee:01", security = Security.WPA2, rssi = -60, frequencyMhz = 2412)

    @Test
    fun openAndWepNetworksRaiseAnAlert_protectedOnesDoNot() = runTest {
        repository.saveScan(listOf(airport, old, cafe), fix = null, time = 1_000)
        assertEquals(listOf("Airport_Free_WiFi", "Old router").sorted(), repository.activeAlerts().first().map { it.displayName }.sorted())
        assertEquals(2, repository.activeAlertCount().first())
    }

    @Test
    fun aNetworkAlertsOnce_evenAfterTheAlertIsDismissed() = runTest {
        repository.saveScan(listOf(airport), fix = null, time = 1_000)
        repository.saveScan(listOf(airport), fix = null, time = 2_000)
        val alert = repository.activeAlerts().first().single()
        repository.dismissAlert(alert.alert.id)
        repository.saveScan(listOf(airport), fix = null, time = 3_000)
        assertEquals(emptyList<AlertWithDevice>(), repository.activeAlerts().first())
    }

    @Test
    fun markingANetworkAsYoursSettlesItsAlert() = runTest {
        val id = repository.saveScan(listOf(airport), fix = null, time = 1_000).single().id
        repository.setStatus(id, DeviceStatus.HOME)
        assertEquals(0, repository.activeAlertCount().first())
    }

    @Test
    fun aTrackerSeenInThreePlacesRaisesOneFollowingAlert() = runTest {
        val tag = BleDevice(address = "da:a1:19:7c:30:02", name = "", rssi = -78, tracker = Tracker(TrackerKind.AIRTAG, separated = true))
        val minute = 60_000L
        // Trinity College, St Stephen's Green, Dublin Castle: 585 to 655 metres apart.
        repository.saveBluetooth(listOf(tag), Fix(53.3445, -6.2590, 12f), time = 0)
        repository.saveBluetooth(listOf(tag), Fix(53.3390, -6.2600, 12f), time = 15 * minute)
        assertEquals(0, repository.activeAlerts().first().size)
        repository.saveBluetooth(listOf(tag), Fix(53.3429, -6.2674, 12f), time = 30 * minute)
        val alert = repository.activeAlerts().first().single()
        assertEquals(AlertKind.FOLLOWING, alert.alert.kind)
        assertEquals(3, alert.alert.places)
        assertEquals("AirTag", alert.displayName)

        // Seen again on the same walk: still one alert.
        repository.saveBluetooth(listOf(tag), Fix(53.3445, -6.2590, 12f), time = 40 * minute)
        assertEquals(1, repository.activeAlerts().first().size)

        // Dismissed, it takes a new walk to alert again.
        repository.dismissAlert(alert.alert.id)
        repository.saveBluetooth(listOf(tag), Fix(53.3390, -6.2600, 12f), time = 50 * minute)
        assertEquals(0, repository.activeAlerts().first().size)
    }
}
