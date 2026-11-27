package tech.vladr.aegis.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnglesTest {
    private val within = 0.001f

    @Test
    fun directionsWrapAround() {
        assertEquals(270f, normalise(-90f), within)
        assertEquals(0f, normalise(360f), within)
        assertEquals(10f, normalise(730f), within)
    }

    @Test
    fun aHairBelowNorthIsNorthNotThreeSixty() {
        val angle = normalise(-0.00001f)
        assertTrue("$angle", angle >= 0f && angle < 360f)
    }

    @Test
    fun turnsTheShortWayAcrossNorth() {
        assertEquals(20f, turn(350f, 10f), within)
        assertEquals(-20f, turn(10f, 350f), within)
    }

    @Test
    fun turnsNoMoreThanHalfWayRound() {
        assertEquals(180f, turn(0f, 180f), within)
        assertEquals(-90f, turn(0f, 270f), within)
        assertEquals(0f, turn(123f, 123f), within)
    }

    @Test
    fun theFirstReadingIsTakenAsItIs() {
        assertEquals(10f, smooth(null, 370f), within)
    }

    @Test
    fun smoothingAcrossNorthStaysNorth() {
        // Halfway between 350 and 10 is north; plain averaging says due south.
        assertEquals(0f, smooth(350f, 10f, fraction = 0.5f), within)
    }

    @Test
    fun smoothingFollowsATurnWithinAFewReadings() {
        var heading: Float? = 0f
        repeat(20) { heading = smooth(heading, 90f) }
        assertEquals(90f, heading!!, 0.5f)
    }

    @Test
    fun smoothingDampsAJitter() {
        // One reading 8 degrees off moves the arrow by only 2.
        assertEquals(2f, smooth(0f, 8f), within)
    }
}
