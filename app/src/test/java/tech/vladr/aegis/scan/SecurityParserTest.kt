package tech.vladr.aegis.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.vladr.aegis.data.Security

/** Capabilities strings as Android reports them for real access points. */
class SecurityParserTest {
    @Test
    fun wpa2Personal() = assertEquals(Security.WPA2, securityOf("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"))

    @Test
    fun wpa2Enterprise_likeEduroam() = assertEquals(Security.WPA2, securityOf("[WPA2-EAP/SHA1-CCMP][RSN-EAP/SHA1-CCMP][ESS]"))

    @Test
    fun wpa3Personal() = assertEquals(Security.WPA3, securityOf("[RSN-SAE-CCMP][ESS][MFPR]"))

    @Test
    fun wpa2AndWpa3Together_countAsWpa3() = assertEquals(Security.WPA3, securityOf("[RSN-PSK+SAE-CCMP][ESS][MFPC]"))

    @Test
    fun wpa3Enterprise192() = assertEquals(Security.WPA3, securityOf("[RSN-EAP-SUITE-B-192-GCMP-256][ESS]"))

    @Test
    fun oldWpaOnly() = assertEquals(Security.WPA, securityOf("[WPA-PSK-TKIP][ESS]"))

    @Test
    fun wpaAndWpa2Together_countAsWpa2() = assertEquals(Security.WPA2, securityOf("[WPA-PSK-TKIP+CCMP][WPA2-PSK-TKIP+CCMP][ESS]"))

    @Test
    fun wep() = assertEquals(Security.WEP, securityOf("[WEP][ESS]"))

    @Test
    fun enhancedOpen() = assertEquals(Security.OWE, securityOf("[RSN-OWE-CCMP][ESS][MFPR]"))

    @Test
    fun openNetworks() {
        assertEquals(Security.OPEN, securityOf("[ESS]"))
        assertEquals(Security.OPEN, securityOf("[ESS][WPS]"))
        assertEquals(Security.OPEN, securityOf(""))
    }

    @Test
    fun onlyOpenAndWepAreUnsafe() {
        assertTrue(Security.OPEN.isUnsafe)
        assertTrue(Security.WEP.isUnsafe)
        Security.entries.filter { it != Security.OPEN && it != Security.WEP }.forEach { assertFalse(it.name, it.isUnsafe) }
    }
}
