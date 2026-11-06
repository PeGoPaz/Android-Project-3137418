package tech.vladr.aegis.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.sensors.Fix

/**
 * The one place the screens go to for data. It turns what a scan found
 * into rows of the database, so no screen needs to know the tables.
 */
class AegisRepository(private val db: AegisDatabase) {

    fun devices(): Flow<List<Device>> = db.devices().observeAll()

    /** Sightings with a location, oldest first: what the map is drawn from. */
    fun locatedSightings(): Flow<List<Sighting>> = db.sightings().observeLocated()

    fun device(id: Long): Flow<Device?> = db.devices().observe(id)

    /** One device's history, newest first. */
    fun sightings(deviceId: Long): Flow<List<Sighting>> = db.sightings().observeForDevice(deviceId)

    suspend fun setStatus(deviceId: Long, status: DeviceStatus) = db.devices().setStatus(deviceId, status)

    /** A blank name goes back to the network's own. */
    suspend fun rename(deviceId: Long, name: String) = db.devices().rename(deviceId, name.trim().ifEmpty { null })

    suspend fun clearHistory(deviceId: Long) = db.sightings().deleteForDevice(deviceId)

    suspend fun deleteDevice(deviceId: Long) = db.devices().delete(deviceId)

    /**
     * Saves a scan: each network becomes a device the first time it is seen
     * and gets a sighting every time, with where the phone was. All in one
     * transaction, so a scan is saved whole or not at all.
     */
    suspend fun saveScan(networks: List<WifiNetwork>, fix: Fix?, time: Long): List<Device> = db.withTransaction {
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
            device
        }
    }
}
