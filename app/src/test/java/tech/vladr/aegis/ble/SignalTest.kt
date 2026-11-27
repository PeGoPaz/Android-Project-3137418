package tech.vladr.aegis.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignalTest {
    private val second = 1_000L

    /** One reading a second, from [from], starting at time 0. */
    private fun everySecond(vararg rssi: Int, from: Long = 0) = rssi.mapIndexed { i, level -> Reading(level, from + i * second) }

    @Test
    fun nothingHeardIsNoLevel() {
        assertNull(levelAt(emptyList(), now = 10 * second))
    }

    @Test
    fun theLevelIsTheMedianOfTheLastFewSeconds() {
        val readings = everySecond(-90, -90, -70, -72, -71, -74)
        // The window ends at 5 s and reaches back 4 s: -90 (1 s), -70, -72, -71, -74.
        assertEquals(-72, levelAt(readings, now = 5 * second))
    }

    @Test
    fun oneReflectionDoesNotMoveTheLevel() {
        val readings = everySecond(-80, -81, -62, -80, -79)
        assertEquals(-80, levelAt(readings, now = 4 * second))
    }

    @Test
    fun anEvenNumberOfReadingsMeetsInTheMiddle() {
        assertEquals(-75, levelAt(everySecond(-70, -80), now = second))
    }

    @Test
    fun readingsOlderThanTheWindowAreLeftOut() {
        assertNull(levelAt(everySecond(-60), now = WINDOW_MS + second))
    }

    @Test
    fun walkingTowardsTheDeviceIsWarmer() {
        val readings = everySecond(-88, -87, -89, -88, -86, -80, -78, -76, -75, -74)
        assertEquals(Trend.WARMER, trendAt(readings, now = 9 * second))
    }

    @Test
    fun walkingAwayIsColder() {
        val readings = everySecond(-70, -71, -70, -72, -70, -77, -80, -82, -83, -84)
        assertEquals(Trend.COLDER, trendAt(readings, now = 9 * second))
    }

    @Test
    fun noiseIsTheSame() {
        val readings = everySecond(-80, -78, -82, -79, -81, -80, -83, -79, -78, -81)
        assertEquals(Trend.SAME, trendAt(readings, now = 9 * second))
    }

    @Test
    fun noTrendUntilThereIsSomethingToCompare() {
        assertNull(trendAt(everySecond(-80, -79, -78), now = 2 * second))
    }

    @Test
    fun onlyTheLastTwoWindowsAreKept() {
        val readings = everySecond(-80, -79, -78, -77, -76, -75, -74, -73, -72, -71)
        val kept = keepRecent(readings, now = 9 * second)
        assertEquals(1 * second, kept.first().at)
        assertEquals(trendAt(readings, 9 * second), trendAt(kept, 9 * second))
    }
}
