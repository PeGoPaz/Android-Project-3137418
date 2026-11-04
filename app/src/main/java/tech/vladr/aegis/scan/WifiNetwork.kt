package tech.vladr.aegis.scan

import tech.vladr.aegis.data.Security

/** A Wi-Fi access point found by one scan. */
data class WifiNetwork(
    /** The network name; empty when the network hides it. */
    val ssid: String,
    /** The access point's MAC address, the same in every scan: how Aegis tells networks apart. */
    val bssid: String,
    val security: Security,
    /** Signal strength in dBm. */
    val rssi: Int,
    val frequencyMhz: Int,
)
