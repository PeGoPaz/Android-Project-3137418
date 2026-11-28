package tech.vladr.aegis.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import tech.vladr.aegis.ble.BleDevice
import tech.vladr.aegis.ble.Tracker
import tech.vladr.aegis.ble.TrackerKind
import tech.vladr.aegis.sensors.Fix

/** The repository tells its listener about a device following you once, after saving the alert. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FollowingListenerTest {
    private lateinit var db: AegisDatabase
    private val told = mutableListOf<Pair<String, Int>>()
    private lateinit var repository: AegisRepository

    private val minute = 60_000L
    private val airtag = BleDevice("de:ad:be:ef:00:01", name = "", rssi = -67, tracker = Tracker(TrackerKind.AIRTAG, separated = true))

    // Public places in Dublin city centre, 585 to 655 metres apart.
    private val trinity = Fix(53.3445, -6.2590, 12f)
    private val stephensGreen = Fix(53.3390, -6.2600, 9f)
    private val castle = Fix(53.3429, -6.2674, 10f)

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AegisDatabase::class.java).build()
        repository = AegisRepository(db) { device, places ->
            // The alert is saved by the time the listener hears of it.
            assertEquals(1, db.alerts().countActive(device.id, AlertKind.FOLLOWING))
            told += device.address to places
        }
    }

    @After
    fun close() = db.close()

    @Test
    fun theThirdPlaceTellsTheListener() = runBlocking {
        repository.saveBluetooth(listOf(airtag), trinity, time = 0)
        repository.saveBluetooth(listOf(airtag), stephensGreen, time = 15 * minute)
        assertEquals(emptyList<Pair<String, Int>>(), told)

        repository.saveBluetooth(listOf(airtag), castle, time = 30 * minute)
        assertEquals(listOf(airtag.address to 3), told)
    }

    @Test
    fun anOpenAlertIsNotToldAgain() = runBlocking {
        listOf(trinity, stephensGreen, castle, trinity).forEachIndexed { i, place ->
            repository.saveBluetooth(listOf(airtag), place, time = i * 15 * minute)
        }
        assertEquals(1, told.size)
    }
}
