package tech.vladr.aegis.data

import androidx.room.Embedded
import tech.vladr.aegis.ble.TrackerKind

/** An alert together with the device it is about, as the Alerts screen lists them. */
data class AlertWithDevice(
    @Embedded val alert: Alert,
    val deviceName: String,
    val deviceCustomName: String?,
    val deviceAddress: String,
    val deviceSecurity: Security?,
    val deviceKind: DeviceKind,
    val deviceTracker: TrackerKind?,
) {
    /** The same name the device's own page shows. */
    val displayName: String
        get() = Device(
            address = deviceAddress, name = deviceName, customName = deviceCustomName,
            kind = deviceKind, tracker = deviceTracker, firstSeen = 0, lastSeen = 0,
        ).displayName
}
