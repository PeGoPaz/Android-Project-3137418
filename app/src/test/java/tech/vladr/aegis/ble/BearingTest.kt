package tech.vladr.aegis.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BearingTest {
    /**
     * A full turn on the spot, a reading every [step] degrees, starting from
     * [from]; the signal is [strong] within 15 degrees either side of
     * [towards] and [weak] the rest of the way round.
     */
    private fun sweep(towards: Float, strong: Int = -62, weak: Int = -74, step: Int = 10, from: Int = 0, turn: Int = 360) =
        (from until from + turn step step).map { degrees ->
            val facing = (degrees % 360).toFloat()
            val off = minOf(kotlin.math.abs(facing - towards), 360 - kotlin.math.abs(facing - towards))
            Sample(facing, if (off <= 15) strong else weak, at = degrees * 50L)
        }

    @Test
    fun aFullTurnFindsTheStrongSide() {
        // 195 is the middle of the 180-210 slice.
        assertEquals(Bearing(195f, 12), bearingOf(sweep(towards = 195f)))
    }

    @Test
    fun theStrongSideJustWestOfNorthIsFound() {
        // Strong at 340, 350 and 0: the 330-360 slice has two of them, the 0-30 slice one.
        assertEquals(345f, bearingOf(sweep(towards = 350f))!!.degrees)
    }

    @Test
    fun halfATurnIsNotEnough() {
        // Only the strong side looked at: of course it is the strongest.
        assertNull(bearingOf(sweep(towards = 90f, turn = 180)))
    }

    @Test
    fun twoThirdsOfATurnIsEnough() {
        assertEquals(105f, bearingOf(sweep(towards = 100f, turn = 240))!!.degrees)
    }

    @Test
    fun noSideStandingOutIsNoBearing() {
        assertNull(bearingOf(sweep(towards = 90f, strong = -72, weak = -74)))
    }

    @Test
    fun oneReflectionDoesNotWinASlice() {
        // Mostly weak towards 300, with one reading off a window 20 dB up.
        val samples = sweep(towards = 90f) + Sample(305f, -50, at = 0) + Sample(306f, -74, at = 0)
        assertEquals(105f, bearingOf(samples)!!.degrees)
    }

    @Test
    fun theSweepForgetsWhereYouStoodBefore() {
        val old = Sample(10f, -60, at = 0)
        val recent = Sample(20f, -70, at = SWEEP_MS + 1_000)
        assertEquals(listOf(recent), keepSweep(listOf(old, recent), now = SWEEP_MS + 1_000))
    }
}
