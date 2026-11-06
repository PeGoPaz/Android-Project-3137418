package tech.vladr.aegis.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Security

class UnsafeNetworkRuleTest {
    private fun network(security: Security?, status: DeviceStatus = DeviceStatus.UNKNOWN, kind: DeviceKind = DeviceKind.WIFI_AP) =
        Device(address = "aa", name = "Net", kind = kind, security = security, status = status, firstSeen = 0, lastSeen = 0)

    @Test
    fun openAndWepNetworksAreUnsafe() {
        assertTrue(isUnsafeNetwork(network(Security.OPEN)))
        assertTrue(isUnsafeNetwork(network(Security.WEP)))
        assertTrue(isUnsafeNetwork(network(Security.OPEN, DeviceStatus.SUSPICIOUS)))
    }

    @Test
    fun protectedNetworksAreNot() {
        listOf(Security.OWE, Security.WPA, Security.WPA2, Security.WPA3, Security.UNKNOWN, null).forEach {
            assertFalse("$it", isUnsafeNetwork(network(it)))
        }
    }

    @Test
    fun yourOwnNetworksNeverAlert() {
        assertFalse(isUnsafeNetwork(network(Security.OPEN, DeviceStatus.TRUSTED)))
        assertFalse(isUnsafeNetwork(network(Security.WEP, DeviceStatus.HOME)))
    }

    @Test
    fun onlyWifiNetworks() = assertFalse(isUnsafeNetwork(network(Security.OPEN, kind = DeviceKind.BLE)))

    @Test
    fun theReasonNamesTheProblem() {
        assertTrue(unsafeReason(Security.OPEN).startsWith("Open network"))
        assertTrue(unsafeReason(Security.WEP).startsWith("WEP"))
        assertEquals("This network is not protected well.", unsafeReason(Security.WPA2))
    }
}
