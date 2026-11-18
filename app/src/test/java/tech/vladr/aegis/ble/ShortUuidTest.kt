package tech.vladr.aegis.ble

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortUuidTest {
    private fun uuid(text: String) = UUID.fromString(text)

    @Test
    fun aBluetoothSigUuidHasA16BitNumber() {
        assertEquals(0xFEED, shortUuid(uuid("0000FEED-0000-1000-8000-00805F9B34FB")))
        assertEquals(0xFD5A, shortUuid(uuid("0000fd5a-0000-1000-8000-00805f9b34fb")))
        assertEquals(0x180F, shortUuid(uuid("0000180F-0000-1000-8000-00805F9B34FB")))
    }

    @Test
    fun otherUuidsHaveNone() {
        // A company's own UUID, here AirGuard's note of Apple's sound service.
        assertNull(shortUuid(uuid("7DFC9000-7D1C-4951-86AA-8D9728F8D66C")))
        // A 32-bit SIG number, and the base with a changed tail.
        assertNull(shortUuid(uuid("1234FEED-0000-1000-8000-00805F9B34FB")))
        assertNull(shortUuid(uuid("0000FEED-0000-1000-8000-00805F9B34FC")))
        assertNull(shortUuid(uuid("0000FEED-0000-1001-8000-00805F9B34FB")))
    }
}
