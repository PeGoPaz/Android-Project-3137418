package tech.vladr.aegis.ui.find

import org.junit.Assert.assertEquals
import org.junit.Test

class FindWordsTest {
    @Test
    fun theMeterFillsFromFarToNear() {
        assertEquals(0f, closeness(-100), 0.001f)
        assertEquals(0.5f, closeness(-70), 0.001f)
        assertEquals(1f, closeness(-40), 0.001f)
    }

    @Test
    fun theMeterStaysInsideItsRing() {
        assertEquals(0f, closeness(-120), 0.001f)
        assertEquals(1f, closeness(-20), 0.001f)
    }

    @Test
    fun distanceInWordsNotMetres() {
        assertEquals("Not heard", distanceLabel(null))
        assertEquals("Right next to you", distanceLabel(-50))
        assertEquals("Close", distanceLabel(-67))
        assertEquals("A few metres away", distanceLabel(-70))
        assertEquals("Further away", distanceLabel(-85))
        assertEquals("Barely heard", distanceLabel(-95))
    }

    @Test
    fun pointsOfTheCompass() {
        assertEquals("N", compassPoint(0f))
        assertEquals("N", compassPoint(22f))
        assertEquals("NE", compassPoint(23f))
        assertEquals("SW", compassPoint(225f))
        assertEquals("NW", compassPoint(300f))
        assertEquals("N", compassPoint(359.9f))
    }
}
