package tech.vladr.aegis.data

import androidx.room.Embedded

/** An alert together with the device it is about, as the Alerts screen lists them. */
data class AlertWithDevice(
    @Embedded val alert: Alert,
    val deviceName: String,
    val deviceCustomName: String?,
    val deviceAddress: String,
    val deviceSecurity: Security?,
) {
    val displayName: String
        get() = deviceCustomName ?: deviceName.ifEmpty { "Hidden network" }
}
