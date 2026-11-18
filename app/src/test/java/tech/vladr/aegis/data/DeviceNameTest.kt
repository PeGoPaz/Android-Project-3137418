package tech.vladr.aegis.data

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.vladr.aegis.ble.TrackerKind

class DeviceNameTest {
    private val network = Device(address = "ac:f8:cc:05:0f:a8", name = "Cafe", kind = DeviceKind.WIFI_AP, firstSeen = 0, lastSeen = 0)
    private val bluetooth = Device(address = "da:a1:19:7c:30:02", name = "", kind = DeviceKind.BLE, firstSeen = 0, lastSeen = 0)

    @Test
    fun yourNameComesFirstThenItsOwn() {
        assertEquals("Cafe", network.displayName)
        assertEquals("Upstairs", network.copy(customName = "Upstairs").displayName)
        assertEquals("Keys", bluetooth.copy(tracker = TrackerKind.AIRTAG, customName = "Keys").displayName)
    }

    @Test
    fun withoutANameItSaysWhatItIs() {
        assertEquals("Hidden network", network.copy(name = "").displayName)
        assertEquals("AirTag", bluetooth.copy(tracker = TrackerKind.AIRTAG).displayName)
        assertEquals("Bluetooth device", bluetooth.displayName)
    }
}
