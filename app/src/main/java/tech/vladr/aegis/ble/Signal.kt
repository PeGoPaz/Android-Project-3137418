package tech.vladr.aegis.ble

/**
 * Hotter and colder, from the signal strength of one device.
 *
 * RSSI (received signal strength, in dBm: -40 is next to the phone, -100 is
 * barely there) is noisy. A hand over the phone, a body in between or a
 * reflection off a wall moves a single reading by 10 dB, so one reading says
 * little. A few seconds of them, taken as a median, say a lot more.
 */

/** One advertisement heard from the device being followed: how strong, and when (milliseconds). */
data class Reading(val rssi: Int, val at: Long)

/** Getting closer to the device, further away, or neither for sure. */
enum class Trend { WARMER, COLDER, SAME }

/**
 * The signal level at [now]: the median of the readings in the [WINDOW_MS]
 * before it, or null if none were heard. A median, not an average, so that one
 * reflection 15 dB off does not move it.
 */
fun levelAt(readings: List<Reading>, now: Long): Int? {
    val recent = readings.filter { it.at in (now - WINDOW_MS)..now }.map { it.rssi }.sorted()
    if (recent.isEmpty()) return null
    val middle = recent.size / 2
    return if (recent.size % 2 == 1) recent[middle] else (recent[middle - 1] + recent[middle]) / 2
}

/**
 * Whether the signal at [now] is stronger or weaker than one window earlier.
 * Less than [MARGIN_DB] either way is [Trend.SAME]: within the noise. Null
 * until there are readings in both windows to compare.
 */
fun trendAt(readings: List<Reading>, now: Long): Trend? {
    val current = levelAt(readings, now) ?: return null
    val before = levelAt(readings, now - WINDOW_MS) ?: return null
    return when {
        current - before >= MARGIN_DB -> Trend.WARMER
        before - current >= MARGIN_DB -> Trend.COLDER
        else -> Trend.SAME
    }
}

/** Only the readings [levelAt] and [trendAt] can still look at: two windows back from [now]. */
fun keepRecent(readings: List<Reading>, now: Long): List<Reading> = readings.filter { it.at >= now - 2 * WINDOW_MS }

/** Long enough to hear an AirTag away from its owner, which advertises about every two seconds, twice. */
const val WINDOW_MS = 4_000L

/** Readings of a device that has not moved still spread over about this much. */
const val MARGIN_DB = 4
