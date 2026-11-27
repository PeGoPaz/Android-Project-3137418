package tech.vladr.aegis.sensors

/**
 * Compass sums. A direction is an angle in degrees, clockwise from north, and
 * it wraps around: 359 and 1 are two degrees apart, not 358. Plain arithmetic
 * gets that wrong, so every heading goes through these.
 */

/** The same direction as [degrees], in 0 (included) to 360 (not included). */
fun normalise(degrees: Float): Float {
    val wrapped = ((degrees % FULL_TURN) + FULL_TURN) % FULL_TURN
    // -0.00001 wraps to 359.99999, which a Float rounds up to exactly 360.
    return if (wrapped >= FULL_TURN) 0f else wrapped
}

/**
 * The shortest turn from [from] to [to]: positive is clockwise, negative is
 * anticlockwise, and never more than half a turn. From 350 to 10 is +20.
 */
fun turn(from: Float, to: Float): Float {
    val clockwise = normalise(to - from)
    return if (clockwise > HALF_TURN) clockwise - FULL_TURN else clockwise
}

/**
 * Moves [previous] a [fraction] of the shortest way towards [reading]. The
 * raw sensor jitters by a few degrees; this keeps an arrow steady while still
 * following a real turn within a fraction of a second. Going the short way
 * round matters: averaging 350 and 10 the plain way gives 180, due south.
 */
fun smooth(previous: Float?, reading: Float, fraction: Float = SMOOTHING): Float =
    if (previous == null) normalise(reading) else normalise(previous + turn(previous, reading) * fraction)

private const val FULL_TURN = 360f
private const val HALF_TURN = 180f

/** At about 16 readings a second, a quarter of the way each time settles in a third of a second. */
const val SMOOTHING = 0.25f
