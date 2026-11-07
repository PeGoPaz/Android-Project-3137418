package tech.vladr.aegis.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacAddressTest {
    @Test
    fun validAddresses() {
        assertTrue(isMacAddress("AC:F8:CC:05:0F:A8"))
        assertTrue(isMacAddress("ac-f8-cc-05-0f-a8"))
        assertTrue(isMacAddress("  ac:f8:cc:05:0f:a8 "))
    }

    @Test
    fun invalidAddresses() {
        listOf("", "AC:F8:CC:05:0F", "AC:F8:CC:05:0F:A8:11", "GG:F8:CC:05:0F:A8", "ACF8CC050FA8", "AC:F8:CC:05:0F:A").forEach {
            assertFalse(it, isMacAddress(it))
        }
    }

    @Test
    fun storedLikeAndroidReportsThem() = assertEquals("ac:f8:cc:05:0f:a8", normaliseMac(" AC-F8-CC-05-0F-A8 "))
}
