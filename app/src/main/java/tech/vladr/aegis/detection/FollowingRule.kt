package tech.vladr.aegis.detection

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Sighting

/**
 * Why a device looks as if it is following you: in how many separate places
 * it was seen, and over what time.
 */
data class Following(val places: Int, val from: Long, val to: Long)

/**
 * Whether a device keeps turning up where you go: seen in [MIN_PLACES]
 * places at least [PLACE_RADIUS_M] apart within [WINDOW_MS], over at least
 * [MIN_SPAN_MS]. Something that stays put - a router, a shop's beacon - is
 * only ever seen in one place, so a device seen in several has moved with you.
 * This is the reasoning AirGuard and Apple's own tracking alerts use.
 *
 * It also catches what travels with you on purpose: the Wi-Fi on a bus, a
 * friend's phone. That is why devices you trust or keep at home never count.
 *
 * Sightings without a location, or whose location may be more than
 * [MAX_ACCURACY_M] off, cannot tell places apart and are left out.
 */
fun followingYou(device: Device, sightings: List<Sighting>): Following? {
    if (device.status == DeviceStatus.TRUSTED || device.status == DeviceStatus.HOME) return null
    val located = sightings
        .filter { it.latitude != null && it.longitude != null && (it.accuracy ?: 0f) <= MAX_ACCURACY_M }
        .sortedBy { it.timestamp }
    val latest = located.lastOrNull() ?: return null
    val recent = located.filter { latest.timestamp - it.timestamp <= WINDOW_MS }

    // Each place is where the device was first seen there; a sighting near none of them is a new place.
    val places = mutableListOf<Sighting>()
    for (sighting in recent) {
        if (places.none { metresBetween(it, sighting) < PLACE_RADIUS_M }) places += sighting
    }
    val from = recent.first().timestamp
    if (places.size < MIN_PLACES || latest.timestamp - from < MIN_SPAN_MS) return null
    return Following(places.size, from, latest.timestamp)
}

/** The distance between two located sightings along the Earth's surface (haversine). */
fun metresBetween(a: Sighting, b: Sighting): Double {
    val lat1 = Math.toRadians(a.latitude!!)
    val lat2 = Math.toRadians(b.latitude!!)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(b.longitude!! - a.longitude!!)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(h))
}

const val MIN_PLACES = 3
const val PLACE_RADIUS_M = 200.0
const val MAX_ACCURACY_M = 100f
const val WINDOW_MS = 12 * 60 * 60 * 1000L
const val MIN_SPAN_MS = 10 * 60 * 1000L
private const val EARTH_RADIUS_M = 6_371_000.0
