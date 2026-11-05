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
