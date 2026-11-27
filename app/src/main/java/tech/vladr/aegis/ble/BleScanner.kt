package tech.vladr.aegis.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** A Bluetooth LE device heard during a scan, with the strongest signal it came in at. */
data class BleDevice(val address: String, val name: String, val rssi: Int, val tracker: Tracker?)

/** Something that listens for Bluetooth devices: the phone's radio, or a fake one in tests. */
fun interface BluetoothScanner {
    suspend fun scan(): List<BleDevice>
}

/** Something that keeps listening to one device: the phone's radio, or a fake one in tests. */
fun interface SignalFollower {
    /** Every advertisement heard from the device at [address], until the collection is cancelled. */
    fun follow(address: String): Flow<Reading>
}

/**
 * Listens for Bluetooth LE advertisements through Android's
 * BluetoothLeScanner for a few seconds and returns every device heard,
 * trackers recognised.
 *
 * Unlike Wi-Fi there is no list to ask for: devices are only heard while the
 * scan runs, each advertising a few times a second (an AirTag away from its
 * owner, about every two seconds), so the scan has to last long enough to
 * catch the slow ones. Needs the "nearby devices" permission (BLUETOOTH_SCAN)
 * and Bluetooth turned on.
 */
class BleScanner(
    private val context: Context,
    private val durationMs: Long = SCAN_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) : BluetoothScanner, SignalFollower {
    /** Throws SecurityException without the permission, and IllegalStateException with Bluetooth off or a scan Android refused. */
    override suspend fun scan(): List<BleDevice> {
        val scanner = leScanner()
        val heard = ConcurrentHashMap<String, BleDevice>()
        // Results and failures arrive on a Binder thread, not the one that reads them.
        val failure = AtomicInteger(NO_FAILURE)
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device.address.lowercase()
                val device = BleDevice(
                    address = address,
                    name = result.scanRecord?.deviceName.orEmpty(),
                    rssi = result.rssi,
                    tracker = recogniseTracker(advertisementOf(result.scanRecord)),
                )
                heard.merge(address, device) { old, new -> if (new.rssi > old.rssi) new.copy(tracker = new.tracker ?: old.tracker) else old }
            }

            override fun onScanFailed(errorCode: Int) {
                failure.set(errorCode)
            }
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(null, settings, callback)
        try {
            delay(durationMs)
        } finally {
            stop(scanner, callback)
        }
        if (failure.get() != NO_FAILURE) throw IllegalStateException("Android refused the Bluetooth scan (error ${failure.get()})")
        return heard.values.sortedByDescending { it.rssi }
    }

    /**
     * Keeps scanning, for as long as the collection lasts, for one device only:
     * the filter by address runs in the Bluetooth chip, so the phone is not
     * woken for every other device nearby. Cancelling the collection stops the
     * scan. The flow fails with the same exceptions as [scan].
     *
     * Trackers change their address now and then (an AirTag away from its
     * owner, once a day), so this is for the few minutes of a search, not for
     * recognising a device later.
     */
    override fun follow(address: String): Flow<Reading> = callbackFlow {
        val scanner = leScanner()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(Reading(result.rssi, clock()))
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Android refused the Bluetooth scan (error $errorCode)"))
            }
        }
        // Android only takes the address in capitals; Aegis stores it in lower case.
        val filter = ScanFilter.Builder().setDeviceAddress(address.uppercase()).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(filter), settings, callback)
        awaitClose { stop(scanner, callback) }
    }

    /**
     * Ends a scan, also one Android has ended already: turning Bluetooth off
     * mid-scan stops it, and stopping it again throws IllegalStateException;
     * taking the permission away mid-scan makes it throw SecurityException.
     */
    private fun stop(scanner: BluetoothLeScanner, callback: ScanCallback) {
        try {
            scanner.stopScan(callback)
        } catch (_: SecurityException) {
        } catch (_: IllegalStateException) {
        }
    }

    private fun leScanner(): BluetoothLeScanner {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Aegis does not have the nearby devices permission")
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        return adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: throw IllegalStateException("Bluetooth is off")
    }

    private companion object {
        const val SCAN_MS = 10_000L
        const val NO_FAILURE = 0
    }
}

/** What a ScanRecord says, cut down to what [recogniseTracker] reads. */
fun advertisementOf(record: ScanRecord?): Advertisement {
    if (record == null) return Advertisement()
    val manufacturer = record.manufacturerSpecificData
    return Advertisement(
        manufacturerData = (0 until (manufacturer?.size() ?: 0)).associate { manufacturer!!.keyAt(it) to manufacturer.valueAt(it) },
        serviceData = record.serviceData.orEmpty().mapNotNull { (uuid, data) -> shortUuid(uuid.uuid)?.let { it to data } }.toMap(),
        serviceUuids = record.serviceUuids.orEmpty().mapNotNull { shortUuid(it.uuid) }.toSet(),
    )
}

/**
 * The 16-bit number of a UUID assigned by the Bluetooth SIG, which are all
 * of the form 0000xxxx-0000-1000-8000-00805F9B34FB; null for any other UUID,
 * such as a company's own 128-bit one.
 */
fun shortUuid(uuid: UUID): Int? {
    val high = uuid.mostSignificantBits
    val isShort = high ushr 48 == 0L && (high and 0xFFFFFFFFL) == BASE.mostSignificantBits && uuid.leastSignificantBits == BASE.leastSignificantBits
    return if (isShort) ((high ushr 32) and 0xFFFF).toInt() else null
}

private val BASE: UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB")
