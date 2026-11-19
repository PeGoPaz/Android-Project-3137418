package tech.vladr.aegis.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.Assert.assertTrue
import tech.vladr.aegis.ble.TrackerKind
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Sighting

class FollowingRuleTest {
    private val noon = 1795003200000L // 2026-11-18 12:00 UTC
    private val minute = 60_000L

    // Public places in Dublin city centre, 585 to 655 metres apart.
    private val trinity = 53.3445 to -6.2590
    private val stephensGreen = 53.3390 to -6.2600
    private val castle = 53.3429 to -6.2674

    private val tag = Device(id = 1, address = "da:a1:19:7c:30:02", name = "", kind = DeviceKind.BLE, firstSeen = noon, lastSeen = noon)

    private fun seen(place: Pair<Double, Double>?, at: Long, accuracy: Float? = 12f) =
        Sighting(deviceId = 1, latitude = place?.first, longitude = place?.second, accuracy = accuracy, rssi = -60, timestamp = at)

    private val walk = listOf(
        seen(trinity, noon),
        seen(trinity, noon + 2 * minute),
        seen(stephensGreen, noon + 15 * minute),
        seen(castle, noon + 30 * minute),
    )

    @Test
    fun seenInThreePlacesOnAWalk() {
        assertEquals(Following(places = 3, from = noon, to = noon + 30 * minute), followingYou(tag, walk))
    }

    @Test
    fun sightingsCloseTogetherAreOnePlace() {
        // 55 metres from the gate: the same place, however often.
        val router = (0 until 20).map { seen(if (it % 2 == 0) trinity else 53.3450 to -6.2590, noon + it * 30 * minute) }
        assertNull(followingYou(tag, router))
    }

    @Test
    fun twoPlacesAreNotEnough() {
        assertNull(followingYou(tag, walk.dropLast(1)))
    }

    @Test
    fun threePlacesPassedInAFewMinutesAreNot() {
        val drive = listOf(seen(trinity, noon), seen(stephensGreen, noon + 2 * minute), seen(castle, noon + 4 * minute))
        assertNull(followingYou(tag, drive))
    }

    @Test
    fun yourOwnDevicesNeverCount() {
        assertNull(followingYou(tag.copy(status = DeviceStatus.TRUSTED), walk))
        assertNull(followingYou(tag.copy(status = DeviceStatus.HOME), walk))
        assertEquals(3, followingYou(tag.copy(status = DeviceStatus.SUSPICIOUS), walk)?.places)
    }

    @Test
    fun sightingsThatCannotTellPlacesApartAreLeftOut() {
        assertNull(followingYou(tag, walk.dropLast(1) + seen(null, noon + 30 * minute)))
        // A location from the mobile network, half a kilometre off.
        assertNull(followingYou(tag, walk.dropLast(1) + seen(castle, noon + 30 * minute, accuracy = 500f)))
        // No accuracy given: the location is taken as it is.
        assertEquals(3, followingYou(tag, walk.dropLast(1) + seen(castle, noon + 30 * minute, accuracy = null))?.places)
    }

    @Test
    fun onlyTheLastTwelveHoursCount() {
        val yesterday = listOf(seen(trinity, noon - 20 * 60 * minute), seen(stephensGreen, noon - 19 * 60 * minute))
        assertNull(followingYou(tag, yesterday + seen(castle, noon)))
        assertEquals(3, followingYou(tag, yesterday + seen(castle, noon - 8 * 60 * minute))?.places)
    }

    @Test
    fun theOrderTheSightingsComeInDoesNotMatter() {
        assertEquals(followingYou(tag, walk), followingYou(tag, walk.reversed()))
    }

    @Test
    fun distanceAlongTheEarth() {
        assertEquals(615.2, metresBetween(seen(trinity, 0), seen(stephensGreen, 0)), 0.5)
        assertEquals(0.0, metresBetween(seen(castle, 0), seen(castle, 0)), 0.001)
    }

    @Test
    fun theAlertSaysWhereItWasSeenAndWhatToDo() {
        val airtag = followingReason(4, TrackerKind.AIRTAG)
        assertTrue(airtag.startsWith("This AirTag was seen in 4 places as you moved."))
        assertTrue("Tracker Detect" in airtag)
        assertTrue("its maker's app" in followingReason(3, TrackerKind.TILE).lowercase())
        assertEquals(
            "This device was seen in 3 places as you moved. If it is not yours or someone's with you, " +
                "it may be travelling with you: check your bag, pockets and car.",
            followingReason(3, null),
        )
    }
}
