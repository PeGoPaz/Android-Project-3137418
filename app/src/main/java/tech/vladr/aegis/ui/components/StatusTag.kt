package tech.vladr.aegis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.ui.theme.Home
import tech.vladr.aegis.ui.theme.Suspicious
import tech.vladr.aegis.ui.theme.Trusted
import tech.vladr.aegis.ui.theme.Unknown

/** The colour of each status, the same on the map and in every list. */
fun statusColor(status: DeviceStatus): Color = when (status) {
    DeviceStatus.TRUSTED -> Trusted
    DeviceStatus.HOME -> Home
    DeviceStatus.UNKNOWN -> Unknown
    DeviceStatus.SUSPICIOUS -> Suspicious
}

/** TRUSTED, HOME or SUSPICIOUS next to a name; nothing for devices you have not marked. */
@Composable
fun StatusTag(status: DeviceStatus?) {
    if (status != null && status != DeviceStatus.UNKNOWN) {
        Tag(status.name, statusColor(status))
    }
}
