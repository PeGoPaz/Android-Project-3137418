package tech.vladr.aegis.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import tech.vladr.aegis.ble.BleDevice
import tech.vladr.aegis.detection.followingYou
import tech.vladr.aegis.detection.isUnsafeNetwork
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.scan.isPrivateAddress
import tech.vladr.aegis.sensors.Fix

/** What saving a scan needs: the Live Scan screen depends on this, not on the whole repository. */
interface ScanStore {
    fun devices(): Flow<List<Device>>
    suspend fun saveScan(networks: List<WifiNetwork>, fix: Fix?, time: Long, alerts: Boolean = true): List<Device>
    suspend fun saveBluetooth(devices: List<BleDevice>, fix: Fix?, time: Long): List<Device>
}

/** Told when a device starts following you, once its alert is saved: the notifications listen for this. */
fun interface FollowingListener {
    suspend fun startedFollowing(device: Device, places: Int)
}

/**
 * The one place the screens go to for data. It turns what a scan found
 * into rows of the database, so no screen needs to know the tables.
 */
class AegisRepository(
    private val db: AegisDatabase,
    private val following: FollowingListener = FollowingListener { _, _ -> },
) : ScanStore {

    override fun devices(): Flow<List<Device>> = db.devices().observeAll()

    /** Sightings with a location, oldest first: what the map is drawn from. */
    fun locatedSightings(): Flow<List<Sighting>> = db.sightings().observeLocated()

    fun device(id: Long): Flow<Device?> = db.devices().observe(id)

    /** One device's history, newest first. */
    fun sightings(deviceId: Long): Flow<List<Sighting>> = db.sightings().observeForDevice(deviceId)

    /** Marking a device as yours also settles its alerts: yours never alert. */
    suspend fun setStatus(deviceId: Long, status: DeviceStatus) = db.withTransaction {
        db.devices().setStatus(deviceId, status)
        if (status == DeviceStatus.TRUSTED || status == DeviceStatus.HOME) db.alerts().dismissFor(deviceId)
    }

    /** A blank name goes back to the network's own. */
    suspend fun rename(deviceId: Long, name: String) = db.devices().rename(deviceId, name.trim().ifEmpty { null })

    suspend fun clearHistory(deviceId: Long) = db.sightings().deleteForDevice(deviceId)

    /** Your trusted and home devices, trusted first. */
    fun ownDevices(): Flow<List<Device>> = db.devices().observeWithStatus(listOf(DeviceStatus.TRUSTED, DeviceStatus.HOME))

    /**
     * Adds a device of yours by hand, by its address; one Aegis already knows
     * just takes the name and the status.
     */
    suspend fun addOwnDevice(address: String, name: String, status: DeviceStatus, time: Long) = db.withTransaction {
        val normalised = address.trim().lowercase()
        val known = db.devices().findByAddress(normalised)
        if (known == null) {
            db.devices().insert(
                Device(address = normalised, name = name.trim(), kind = DeviceKind.WIFI_AP, status = status, firstSeen = time, lastSeen = time),
            )
        } else {
            db.devices().update(known.copy(customName = name.trim().ifEmpty { null } ?: known.customName, status = status))
            db.alerts().dismissFor(known.id)
        }
    }

    /** Forgets everything Aegis has saved: devices, sightings and alerts. */
    suspend fun deleteEverything() = db.clearAllTables()

    suspend fun deleteDevice(deviceId: Long) = db.devices().delete(deviceId)

    /**
     * Saves a scan: each network becomes a device the first time it is seen
     * and gets a sighting every time, with where the phone was. An unsafe
     * network gets an alert - once: dismissing it is an answer. All in one
     * transaction, so a scan is saved whole or not at all.
     */
    override suspend fun saveScan(networks: List<WifiNetwork>, fix: Fix?, time: Long, alerts: Boolean): List<Device> {
        val raised = mutableListOf<Pair<Device, Int>>()
        val saved = db.withTransaction {
            networks.map { network ->
                val known = db.devices().findByAddress(network.bssid)
                val device = if (known == null) {
                    val fresh = Device(
                        address = network.bssid,
                        name = network.ssid,
                        kind = DeviceKind.WIFI_AP,
                        security = network.security,
                        firstSeen = time,
                        lastSeen = time,
                    )
                    fresh.copy(id = db.devices().insert(fresh))
                } else {
                    // A network can change its name or protection; keep what it says now.
                    known.copy(name = network.ssid, security = network.security, lastSeen = time).also { db.devices().update(it) }
                }
                db.sightings().insert(
                    Sighting(
                        deviceId = device.id,
                        latitude = fix?.latitude,
                        longitude = fix?.longitude,
                        accuracy = fix?.accuracyMetres,
                        rssi = network.rssi,
                        timestamp = time,
                    ),
                )
                if (alerts && isUnsafeNetwork(device) && db.alerts().countAll(device.id, AlertKind.UNSAFE_NETWORK) == 0) {
                    db.alerts().insert(Alert(deviceId = device.id, kind = AlertKind.UNSAFE_NETWORK, createdAt = time))
                }
                // A network can follow you too: the hotspot of a phone in someone's bag. Only made-up
                // addresses count - a router keeps its maker's address, and a strong one can be heard
                // from places far enough apart to look like three.
                if (isPrivateAddress(device.address)) alertIfFollowing(device, time)?.let { raised += device to it }
                device
            }
        }
        tell(raised)
        return saved
    }

    /**
     * Saves the Bluetooth devices heard in a scan, the same way as networks:
     * a device the first time, a sighting every time. A tracker kind once
     * recognised is kept, since not every advertisement carries it.
     */
    override suspend fun saveBluetooth(devices: List<BleDevice>, fix: Fix?, time: Long): List<Device> {
        val raised = mutableListOf<Pair<Device, Int>>()
        val saved = db.withTransaction {
            devices.map { heard ->
                val known = db.devices().findByAddress(heard.address)
                val device = if (known == null) {
                    val fresh = Device(
                        address = heard.address,
                        name = heard.name,
                        kind = DeviceKind.BLE,
                        tracker = heard.tracker?.kind,
                        firstSeen = time,
                        lastSeen = time,
                    )
                    fresh.copy(id = db.devices().insert(fresh))
                } else {
                    known.copy(name = heard.name.ifEmpty { known.name }, tracker = heard.tracker?.kind ?: known.tracker, lastSeen = time)
                        .also { db.devices().update(it) }
                }
                db.sightings().insert(
                    Sighting(
                        deviceId = device.id,
                        latitude = fix?.latitude,
                        longitude = fix?.longitude,
                        accuracy = fix?.accuracyMetres,
                        rssi = heard.rssi,
                        timestamp = time,
                    ),
                )
                alertIfFollowing(device, time)?.let { raised += device to it }
                device
            }
        }
        tell(raised)
        return saved
    }

    /**
     * Raises a "following you" alert when a device's sightings say it has
     * moved with you. Once a following alert is open, a later scan does not
     * add another. After you dismiss it, only sightings since that alert
     * count, so it takes another outing with the device before it alerts again.
     *
     * @return in how many places the device was seen, when this raised an alert; null when it did not.
     */
    private suspend fun alertIfFollowing(device: Device, time: Long): Int? {
        if (db.alerts().countActive(device.id, AlertKind.FOLLOWING) > 0) return null
        val lastDismissed = db.alerts().lastDismissed(device.id, AlertKind.FOLLOWING)
        val since = db.sightings().forDevice(device.id).filter { lastDismissed == null || it.timestamp > lastDismissed }
        val following = followingYou(device, since) ?: return null
        db.alerts().insert(Alert(deviceId = device.id, kind = AlertKind.FOLLOWING, places = following.places, createdAt = time))
        return following.places
    }

    /** After the transaction, not inside it: a notification for an alert that was rolled back would point at nothing. */
    private suspend fun tell(raised: List<Pair<Device, Int>>) = raised.forEach { (device, places) -> following.startedFollowing(device, places) }

    fun activeAlerts(): Flow<List<AlertWithDevice>> = db.alerts().observeActive()

    fun activeAlertCount(): Flow<Int> = db.alerts().observeActiveCount()

    suspend fun dismissAlert(alertId: Long) = db.alerts().dismiss(alertId)
}
