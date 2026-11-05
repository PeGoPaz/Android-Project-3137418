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
}
