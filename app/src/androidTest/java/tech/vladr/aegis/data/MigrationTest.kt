package tech.vladr.aegis.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Moving a database from one version to the next keeps what was in it. The
 * old version is built from its schema in app/schemas, filled in with plain
 * SQL, and then opened at the new version, which Room checks against the
 * tables it expects. Runs on a phone or emulator.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AegisDatabase::class.java)

    @Test
    fun version1KeepsItsNetworksSightingsAndAlerts() {
        helper.createDatabase(NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO device (id, address, name, customName, kind, security, status, firstSeen, lastSeen) " +
                    "VALUES (1, 'ac:f8:cc:05:0f:a8', 'Cafe', 'Corner cafe', 'WIFI_AP', 'OPEN', 'SUSPICIOUS', 1000, 2000)",
            )
            db.execSQL("INSERT INTO sighting (deviceId, latitude, longitude, accuracy, rssi, timestamp) VALUES (1, 53.3445, -6.259, 12.0, -61, 1000)")
            db.execSQL("INSERT INTO alert (deviceId, kind, places, createdAt, dismissed) VALUES (1, 'UNSAFE_NETWORK', 0, 1000, 0)")
        }

        helper.runMigrationsAndValidate(NAME, 2, true).use { db ->
            db.query("SELECT customName, status, tracker FROM device WHERE id = 1").use { row ->
                assertTrue(row.moveToFirst())
                assertEquals("Corner cafe", row.getString(0))
                assertEquals("SUSPICIOUS", row.getString(1))
                assertTrue("a network is no tracker", row.isNull(2))
            }
            db.query("SELECT COUNT(*) FROM sighting").use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
            db.query("SELECT kind FROM alert").use { assertTrue(it.moveToFirst()); assertEquals("UNSAFE_NETWORK", it.getString(0)) }
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
