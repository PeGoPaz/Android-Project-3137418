package tech.vladr.aegis.ble

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Following one device, against Robolectric's Bluetooth radio. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FollowTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val adapter = app.getSystemService(BluetoothManager::class.java).adapter
    private val tag = "da:a1:19:7c:30:02"

    private fun allowed(bluetoothOn: Boolean = true) {
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_SCAN)
        shadowOf(adapter).setEnabled(bluetoothOn)
    }

    @Test
    fun listensForThatOneDeviceAndStopsWhenTheSearchEnds() = runTest {
        allowed()
        val received = mutableListOf<Reading>()
        val search = launch(UnconfinedTestDispatcher(testScheduler)) {
            BleScanner(app, clock = { 42L }).follow(tag).collect { received += it }
        }

        val radio = shadowOf(adapter.bluetoothLeScanner)
        val scan = radio.activeScans.single()
        assertEquals("DA:A1:19:7C:30:02", scan.scanFilters().single().deviceAddress)

        scan.scanCallback()!!.onScanResult(0, ScanResult(adapter.getRemoteDevice("DA:A1:19:7C:30:02"), null, -63, 0))
        assertEquals(listOf(Reading(-63, 42L)), received)

        search.cancel()
        assertTrue("still scanning", radio.activeScans.isEmpty())
    }

    @Test
    fun aRefusedScanEndsTheSearchWithTheReason() = runTest {
        allowed()
        val search = launch(UnconfinedTestDispatcher(testScheduler)) {
            val failure = runCatching { BleScanner(app).follow(tag).first() }.exceptionOrNull()
            assertTrue("$failure", failure is IllegalStateException && "error 2" in failure.message.orEmpty())
        }
        shadowOf(adapter.bluetoothLeScanner).activeScans.single().scanCallback()!!.onScanFailed(2)
        search.join()
    }

    @Test
    fun withBluetoothOffTheSearchFails() = runTest {
        allowed(bluetoothOn = false)
        val failure = runCatching { BleScanner(app).follow(tag).first() }.exceptionOrNull()
        assertTrue("$failure", failure is IllegalStateException)
    }

    @Test
    fun withoutThePermissionTheSearchFails() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.BLUETOOTH_SCAN)
        val failure = runCatching { BleScanner(app).follow(tag).first() }.exceptionOrNull()
        assertTrue("$failure", failure is SecurityException)
    }
}
