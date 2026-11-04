package tech.vladr.aegis.scan

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.vladr.aegis.ui.components.signalLevel

class SignalLevelTest {
    @Test
    fun barsByStrength() {
        assertEquals(4, signalLevel(-41))
        assertEquals(4, signalLevel(-55))
        assertEquals(3, signalLevel(-63))
        assertEquals(2, signalLevel(-72))
        assertEquals(1, signalLevel(-85))
        assertEquals(0, signalLevel(-92))
    }
}
