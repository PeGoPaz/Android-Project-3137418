package tech.vladr.aegis.ui.map

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Sighting
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.scan.isUnsafe

data class MapPoint(val latitude: Double, val longitude: Double)

/** A network on the map: where it probably is, and how to colour it. */
data class DeviceMarker(val deviceId: Long, val name: String, val status: DeviceStatus, val unsafe: Boolean, val at: MapPoint)

/** Your route: one point per scan with a location, in the order you scanned. */
fun route(sightings: List<Sighting>): List<MapPoint> =
    sightings
        .filter { it.latitude != null && it.longitude != null }
        .sortedBy { it.timestamp }
        .distinctBy { it.timestamp } // every network of a scan shares the scan's location
        .map { MapPoint(it.latitude!!, it.longitude!!) }
        .fold(emptyList()) { points, point -> if (points.lastOrNull() == point) points else points + point }

/**
 * Where each device probably is. A scan says that a network was in reach,
 * not in which direction, so the best guess is the places it was heard
 * from, each counted by how strong it was there: power in milliwatts,
 * 10^(dBm/10), so a signal 10 dB stronger counts ten times as much.
 *
 * All the networks of one scan would then sit on one point, so each is
 * moved a few metres from it, in a direction taken from its address: the
 * same every time, and different for each network.
 */
fun placeDevices(devices: List<Device>, sightings: List<Sighting>): List<DeviceMarker> {
    val heard = sightings.filter { it.latitude != null && it.longitude != null }.groupBy { it.deviceId }
    return devices.mapNotNull { device ->
        val places = heard[device.id] ?: return@mapNotNull null
        val weights = places.map { 10.0.pow(it.rssi / 10.0) }
        val total = weights.sum()
        val centre = MapPoint(
            latitude = places.zip(weights).sumOf { (s, w) -> s.latitude!! * w } / total,
            longitude = places.zip(weights).sumOf { (s, w) -> s.longitude!! * w } / total,
        )
        DeviceMarker(
            deviceId = device.id,
            name = device.displayName,
            status = device.status,
            unsafe = (device.security ?: Security.UNKNOWN).isUnsafe,
            at = spread(centre, device.address),
        )
    }
}

/** A point 8 to 20 metres from [centre], in a direction and at a distance that depend only on [key]. */
fun spread(centre: MapPoint, key: String): MapPoint {
    val hash = key.hashCode().toLong() and 0xffffffffL
    val angle = Math.toRadians((hash % 360).toDouble())
    val metres = 8.0 + (hash / 360 % 13)
    val metresPerDegree = 111_320.0
    return MapPoint(
        latitude = centre.latitude + metres * cos(angle) / metresPerDegree,
        longitude = centre.longitude + metres * sin(angle) / (metresPerDegree * cos(Math.toRadians(centre.latitude))),
    )
}
