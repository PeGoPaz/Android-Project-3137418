package tech.vladr.aegis.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.data.Sighting
import kotlin.math.cos
import kotlin.math.hypot

class PlacementTest {
    private fun device(id: Long, address: String, security: Security = Security.WPA2) =
        Device(id = id, address = address, name = "Net $id", kind = DeviceKind.WIFI_AP, security = security, firstSeen = 0, lastSeen = 0)

    private fun seen(deviceId: Long, lat: Double?, lon: Double?, rssi: Int, time: Long) =
        Sighting(deviceId = deviceId, latitude = lat, longitude = lon, accuracy = 5f, rssi = rssi, timestamp = time)

    /** Distance in metres, flat-earth: plenty for a few hundred metres. */
    private fun metres(a: MapPoint, b: MapPoint): Double {
        val dy = (a.latitude - b.latitude) * 111_320
        val dx = (a.longitude - b.longitude) * 111_320 * cos(Math.toRadians(a.latitude))
        return hypot(dx, dy)
    }

    @Test
    fun theRouteHasOnePointPerScan_inOrder() {
        val sightings = listOf(
            seen(1, 53.0, -6.0, -60, time = 2),
            seen(2, 53.0, -6.0, -70, time = 2),
            seen(1, 52.9, -6.1, -60, time = 1),
            seen(1, null, null, -60, time = 3),
        )
        assertEquals(listOf(MapPoint(52.9, -6.1), MapPoint(53.0, -6.0)), route(sightings))
    }

    @Test
    fun standingStillAddsNoPointsToTheRoute() {
        val sightings = listOf(seen(1, 53.0, -6.0, -60, time = 1), seen(1, 53.0, -6.0, -60, time = 2))
        assertEquals(1, route(sightings).size)
    }

    @Test
    fun aDeviceIsPlacedNearerWhereItWasHeardStronger() {
        val a = MapPoint(53.3500, -6.2600)
        val b = MapPoint(53.3510, -6.2600) // about 111 m north
        val markers = placeDevices(
            listOf(device(1, "aa:bb:cc:dd:ee:01")),
            listOf(seen(1, a.latitude, a.longitude, rssi = -50, time = 1), seen(1, b.latitude, b.longitude, rssi = -80, time = 2)),
        )
        val at = markers.single().at
        assertTrue("closer to the strong side", metres(at, a) < metres(at, b))
    }

    @Test
    fun devicesOfOneScanAreSpreadAFewMetresApart() {
        val here = MapPoint(53.35, -6.26)
        val devices = (1L..20L).map { device(it, "aa:bb:cc:dd:ee:%02x".format(it)) }
        val markers = placeDevices(devices, devices.map { seen(it.id, here.latitude, here.longitude, -60, time = 1) })
        markers.forEach { assertTrue(metres(it.at, here) in 7.9..20.1) }
        assertTrue("not on one spot", markers.map { it.at }.toSet().size > 15)
    }

    @Test
    fun theSpreadIsTheSameEveryTime() = assertEquals(spread(MapPoint(53.35, -6.26), "aa:bb"), spread(MapPoint(53.35, -6.26), "aa:bb"))

    @Test
    fun devicesWithoutALocationAreNotOnTheMap_andUnsafeOnesAreMarked() {
        val markers = placeDevices(
            listOf(device(1, "a", Security.OPEN), device(2, "b")),
            listOf(seen(1, 53.0, -6.0, -60, time = 1), seen(2, null, null, -60, time = 1)),
        )
        assertEquals(listOf(1L), markers.map { it.deviceId })
        assertTrue(markers.single().unsafe)
        assertEquals(DeviceStatus.UNKNOWN, markers.single().status)
    }
}
