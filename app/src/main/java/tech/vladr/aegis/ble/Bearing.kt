package tech.vladr.aegis.ble

import tech.vladr.aegis.sensors.normalise

/**
 * Which way a device is, from one person turning slowly on the spot.
 *
 * A phone has one antenna and cannot tell where a signal comes from. A human
 * body can: it is mostly water, and water soaks up 2.4 GHz radio, so with the
 * phone held in front of you the signal is weakest with the device behind you
 * and strongest facing it. Turning a full circle, with the compass saying which
 * way the phone faced at every reading, turns that into a direction. Radio
 * amateurs hunting for a hidden transmitter call it the "body fade".
 */

/** One reading during a sweep: which way the phone faced, how strong the signal was, and when (milliseconds). */
data class Sample(val heading: Float, val rssi: Int, val at: Long)

/**
 * Where the signal was strongest: [degrees] from north, the middle of the best
 * [SECTOR_DEGREES] slice, and by how many dB that slice beat the others,
 * [contrastDb]: the higher, the surer.
 */
data class Bearing(val degrees: Float, val contrastDb: Int)

/**
 * The bearing of the device from a sweep's [samples], or null when they cannot
 * say yet: when less than [MIN_SECTORS] slices of the circle were faced (the
 * strongest could just be the only side looked at), or when the best slice is
 * less than [MIN_CONTRAST_DB] above the median of the rest (nothing stands out).
 * Each slice counts by the median of its readings, so one reflection does not win it.
 */
fun bearingOf(samples: List<Sample>): Bearing? {
    val sectors = samples
        .groupBy { (normalise(it.heading) / SECTOR_DEGREES).toInt() % SECTORS }
        .mapValues { (_, inSector) -> median(inSector.map { it.rssi }) }
    if (sectors.size < MIN_SECTORS) return null
    val best = sectors.maxBy { it.value }
    val rest = median(sectors.filterKeys { it != best.key }.values.toList())
    val contrast = best.value - rest
    if (contrast < MIN_CONTRAST_DB) return null
    return Bearing(best.key * SECTOR_DEGREES + SECTOR_DEGREES / 2, contrast)
}

/** Only the samples of the last [SWEEP_MS]: after a few steps, older ones were taken somewhere else. */
fun keepSweep(samples: List<Sample>, now: Long): List<Sample> = samples.filter { it.at >= now - SWEEP_MS }

private fun median(values: List<Int>): Int {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

/** Twelve slices, like the hours on a clock face. */
const val SECTOR_DEGREES = 30f
private const val SECTORS = 12

/** Two thirds of a full turn. */
const val MIN_SECTORS = 8

/** A body between phone and device costs it about 5 to 15 dB. */
const val MIN_CONTRAST_DB = 5

/** Long enough for a slow full turn, short enough to forget the last spot after walking on. */
const val SWEEP_MS = 20_000L
