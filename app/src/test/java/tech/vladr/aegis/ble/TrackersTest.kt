package tech.vladr.aegis.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackersTest {
    /** Bytes written the way Bluetooth tools print them: "12 19 10 ...". */
    private fun bytes(hex: String): ByteArray = hex.split(' ').map { it.toInt(16).toByte() }.toByteArray()

    /** An Apple Find My message: type, length, status byte, then the rest of the key. */
    private fun findMy(length: Int, status: Int) =
        Advertisement(manufacturerData = mapOf(0x004C to bytes("12 %02x %02x 5c 8a 3b 1f".format(length, status))))

    @Test
    fun appleTellsWhatTheDeviceIsInItsStatusByte() {
        assertEquals(TrackerKind.AIRTAG, recogniseTracker(findMy(0x19, 0x10))?.kind)
        assertEquals(TrackerKind.FIND_MY_ACCESSORY, recogniseTracker(findMy(0x19, 0x20))?.kind)
        assertEquals(TrackerKind.AIRPODS, recogniseTracker(findMy(0x19, 0x30))?.kind)
        assertEquals(TrackerKind.APPLE_DEVICE, recogniseTracker(findMy(0x19, 0x00))?.kind)
        // The battery is in the top two bits; it does not change what the device is.
        assertEquals(TrackerKind.AIRTAG, recogniseTracker(findMy(0x19, 0xd0))?.kind)
    }

    @Test
    fun anAppleDeviceAwayFromItsOwnerSendsTheLongMessage() {
        assertEquals(Tracker(TrackerKind.AIRTAG, separated = true), recogniseTracker(findMy(0x19, 0x10)))
        assertEquals(Tracker(TrackerKind.AIRTAG, separated = false), recogniseTracker(findMy(0x02, 0x10)))
    }

    @Test
    fun otherAppleMessagesAreNotTrackers() {
        // 0x10 is "nearby info", which every iPhone sends.
        assertNull(recogniseTracker(Advertisement(manufacturerData = mapOf(0x004C to bytes("10 05 0b 1c 4f 2e 91")))))
        assertNull(recogniseTracker(Advertisement(manufacturerData = mapOf(0x004C to bytes("12 19")))))
        // The same bytes from another company are not Apple's.
        assertNull(recogniseTracker(Advertisement(manufacturerData = mapOf(0x0075 to bytes("12 19 10 5c")))))
    }

    @Test
    fun googleFindMyDeviceTrackersUseTheirOwnEddystoneFrames() {
        val eddystone = { frame: String -> Advertisement(serviceData = mapOf(0xFEAA to bytes("$frame 6e 21 9c 04"))) }
        assertEquals(Tracker(TrackerKind.GOOGLE_FIND_MY, separated = false), recogniseTracker(eddystone("40")))
        assertEquals(Tracker(TrackerKind.GOOGLE_FIND_MY, separated = true), recogniseTracker(eddystone("41")))
        // UID, URL, TLM and EID beacons: shop and museum beacons, not trackers.
        for (frame in listOf("00", "10", "20", "30")) assertNull(frame, recogniseTracker(eddystone(frame)))
    }

    @Test
    fun otherTrackersAreKnownByTheirService() {
        assertEquals(Tracker(TrackerKind.SMARTTAG), recogniseTracker(Advertisement(serviceData = mapOf(0xFD5A to bytes("10 2a 00 01")))))
        assertEquals(Tracker(TrackerKind.TILE), recogniseTracker(Advertisement(serviceUuids = setOf(0xFEED))))
        assertEquals(Tracker(TrackerKind.CHIPOLO), recogniseTracker(Advertisement(serviceUuids = setOf(0x180F, 0xFE33))))
        assertEquals(Tracker(TrackerKind.PEBBLEBEE), recogniseTracker(Advertisement(serviceUuids = setOf(0xFA25))))
    }

    @Test
    fun everythingElseIsNotATracker() {
        assertNull(recogniseTracker(Advertisement()))
        // A heart rate strap and a battery service.
        assertNull(recogniseTracker(Advertisement(serviceUuids = setOf(0x180D, 0x180F))))
    }
}
