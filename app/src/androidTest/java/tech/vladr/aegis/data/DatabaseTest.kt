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

/**
 * The tables and queries, on a real SQLite database in memory. Runs on a
 * phone or emulator: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AegisDatabase

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AegisDatabase::class.java).build()
    }

    @After
    fun close() = db.close()

    private fun network(address: String, name: String, security: Security = Security.WPA2) =
        Device(address = address, name = name, kind = DeviceKind.WIFI_AP, security = security, firstSeen = 1_000, lastSeen = 1_000)

    @Test
    fun aDeviceIsFoundByItsAddress() = runTest {
        val id = db.devices().insert(network("aa:bb:cc:dd:ee:01", "Cafe"))
        assertEquals(id, db.devices().findByAddress("aa:bb:cc:dd:ee:01")?.id)
        assertNull(db.devices().findByAddress("aa:bb:cc:dd:ee:99"))
    }

    @Test
    fun statusAndNameCanBeChanged() = runTest {
        val id = db.devices().insert(network("aa:bb:cc:dd:ee:01", "Cafe"))
        db.devices().setStatus(id, DeviceStatus.HOME)
        db.devices().rename(id, "My router")
        val device = db.devices().observe(id).first()!!
        assertEquals(DeviceStatus.HOME, device.status)
        assertEquals("My router", device.displayName)
    }

    @Test
    fun deletingADeviceDeletesItsSightingsAndAlerts() = runTest {
        val id = db.devices().insert(network("aa:bb:cc:dd:ee:01", "Airport", Security.OPEN))
        db.sightings().insert(Sighting(deviceId = id, latitude = 53.33, longitude = -6.27, accuracy = 5f, rssi = -60, timestamp = 2_000))
        db.alerts().insert(Alert(deviceId = id, kind = AlertKind.UNSAFE_NETWORK, createdAt = 2_000))
        db.devices().delete(id)
        assertEquals(emptyList<Sighting>(), db.sightings().observeForDevice(id).first())
        assertEquals(emptyList<AlertWithDevice>(), db.alerts().observeActive().first())
    }

    @Test
    fun theMapGetsOnlySightingsWithALocation_oldestFirst() = runTest {
        val id = db.devices().insert(network("aa:bb:cc:dd:ee:01", "Cafe"))
        db.sightings().insert(Sighting(deviceId = id, latitude = 53.34, longitude = -6.26, accuracy = 5f, rssi = -60, timestamp = 3_000))
        db.sightings().insert(Sighting(deviceId = id, latitude = null, longitude = null, accuracy = null, rssi = -70, timestamp = 2_500))
        db.sightings().insert(Sighting(deviceId = id, latitude = 53.33, longitude = -6.27, accuracy = 5f, rssi = -65, timestamp = 2_000))
        assertEquals(listOf(2_000L, 3_000L), db.sightings().observeLocated().first().map { it.timestamp })
    }

    @Test
    fun aDismissedAlertIsNoLongerActive() = runTest {
        val id = db.devices().insert(network("aa:bb:cc:dd:ee:01", "Airport", Security.OPEN))
        val alert = db.alerts().insert(Alert(deviceId = id, kind = AlertKind.UNSAFE_NETWORK, createdAt = 2_000))
        assertEquals(1, db.alerts().countActive(id, AlertKind.UNSAFE_NETWORK))
        assertEquals("Airport", db.alerts().observeActive().first().single().displayName)
        db.alerts().dismiss(alert)
        assertEquals(0, db.alerts().countActive(id, AlertKind.UNSAFE_NETWORK))
    }
}
