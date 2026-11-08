package tech.vladr.aegis.ui.detail

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.data.Sighting

class ReportTest {
    private val nov8 = 1794171240000L // 2026-11-08 20:54 UTC

    private val airport = Device(
        id = 7, address = "aa:bb:cc:dd:ee:02", name = "Airport_Free_WiFi", kind = DeviceKind.WIFI_AP,
        security = Security.OPEN, status = DeviceStatus.SUSPICIOUS, firstSeen = nov8, lastSeen = nov8 + 300_000,
    )

    @Test
    fun theReportReadsWithoutAegis() {
        val report = deviceReport(
            airport,
            listOf(
                Sighting(deviceId = 7, latitude = 53.31795, longitude = -6.24105, accuracy = 16f, rssi = -46, timestamp = nov8),
                Sighting(deviceId = 7, latitude = null, longitude = null, accuracy = null, rssi = -41, timestamp = nov8 + 300_000),
            ),
            ZoneOffset.UTC,
        )
        assertEquals(
            """
            Aegis report: Airport_Free_WiFi

            BSSID: AA:BB:CC:DD:EE:02
            Protection: open (unsafe)
            Status: suspicious
            First seen: 2026-11-08 20:54
            Last seen: 2026-11-08 20:59
            Seen 2 times, strongest signal -41 dBm

            Sightings, newest first:
            - 2026-11-08 20:59  no location  -41 dBm
            - 2026-11-08 20:54  53.31795, -6.24105 (±16 m)  -46 dBm

            Made with Aegis, a privacy-first Wi-Fi scanner.
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun aRenamedNetworkKeepsItsOwnNameInTheReport() {
        val report = deviceReport(airport.copy(customName = "Gate 12 wifi", security = Security.WPA2, status = DeviceStatus.UNKNOWN), emptyList(), ZoneOffset.UTC)
        assertTrue(report.startsWith("Aegis report: Gate 12 wifi\n\nNetwork name: Airport_Free_WiFi\n"))
        assertTrue("Protection: WPA2\n" in report)
        assertTrue("Seen 0 times\n" in report)
    }

    @Test
    fun once() = assertEquals("once", times(1))
}
