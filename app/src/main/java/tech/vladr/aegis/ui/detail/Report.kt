package tech.vladr.aegis.ui.detail

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.data.Sighting
import tech.vladr.aegis.scan.isUnsafe
import tech.vladr.aegis.ui.scan.securityLabel

/**
 * A device and its history as plain text, for Share report: something a
 * person can paste into a message or an email and read without Aegis.
 * Sharing is the only way data leaves the phone, and only when you choose.
 */
fun deviceReport(device: Device, sightings: List<Sighting>, zone: ZoneId = ZoneId.systemDefault()): String {
    val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(zone)
    val security = device.security ?: Security.UNKNOWN
    return buildString {
        appendLine("Aegis report: ${device.displayName}")
        appendLine()
        if (device.customName != null && device.name.isNotEmpty()) appendLine("Network name: ${device.name}")
        appendLine("BSSID: ${device.address.uppercase()}")
        appendLine("Protection: ${securityLabel(security)}" + if (security.isUnsafe) " (unsafe)" else "")
        appendLine("Status: ${device.status.name.lowercase()}")
        appendLine("First seen: ${time.format(Instant.ofEpochMilli(device.firstSeen))}")
        appendLine("Last seen: ${time.format(Instant.ofEpochMilli(device.lastSeen))}")
        appendLine("Seen ${times(sightings.size)}" + (sightings.maxOfOrNull { it.rssi }?.let { ", strongest signal $it dBm" } ?: ""))
        if (sightings.isNotEmpty()) {
            appendLine()
            appendLine("Sightings, newest first:")
            sightings.sortedByDescending { it.timestamp }.forEach { s ->
                val where = if (s.latitude == null || s.longitude == null) {
                    "no location"
                } else {
                    "%.5f, %.5f".format(Locale.ROOT, s.latitude, s.longitude) + (s.accuracy?.let { " (±${it.roundToInt()} m)" } ?: "")
                }
                appendLine("- ${time.format(Instant.ofEpochMilli(s.timestamp))}  $where  ${s.rssi} dBm")
            }
        }
        appendLine()
        append("Made with Aegis, a privacy-first Wi-Fi scanner.")
    }
}

/** "once", "2 times". */
fun times(n: Int): String = if (n == 1) "once" else "$n times"
