package tech.vladr.aegis.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import tech.vladr.aegis.ble.TrackerKind

/**
 * One network or device Aegis has seen, however many times. Its sightings
 * hold where and when.
 */
@Entity(tableName = "device", indices = [Index(value = ["address"], unique = true)])
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The BSSID of a Wi-Fi access point, or the address of a Bluetooth device. */
    val address: String,
    /** The network name (SSID) or the name the device advertises; empty for hidden networks. */
    val name: String,
    /** A name you gave it, shown instead of [name]. */
    val customName: String? = null,
    val kind: DeviceKind,
    /** Wi-Fi only. */
    val security: Security? = null,
    val status: DeviceStatus = DeviceStatus.UNKNOWN,
    val firstSeen: Long,
    val lastSeen: Long,
    /** Bluetooth only: what kind of tracker it advertised itself as, if any. Since version 2. */
    val tracker: TrackerKind? = null,
) {
    /**
     * The name to show: yours, else its own, else what it is. Trackers
     * advertise no name, and a Wi-Fi network without one is hidden.
     */
    val displayName: String
        get() = customName ?: name.ifEmpty {
            when {
                tracker != null -> tracker.label
                kind == DeviceKind.BLE -> "Bluetooth device"
                else -> "Hidden network"
            }
        }
}
