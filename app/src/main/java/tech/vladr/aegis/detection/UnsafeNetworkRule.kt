package tech.vladr.aegis.detection

import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.scan.isUnsafe

/**
 * Whether a network deserves a warning: an open or WEP network you have not
 * marked as yours. On an open network anyone nearby can read what is not
 * encrypted by the app itself; WEP can be broken in minutes.
 */
fun isUnsafeNetwork(device: Device): Boolean =
    device.kind == DeviceKind.WIFI_AP &&
        device.security?.isUnsafe == true &&
        device.status != DeviceStatus.TRUSTED &&
        device.status != DeviceStatus.HOME

/** What an alert says about a network, in words. */
fun unsafeReason(security: Security?): String = when (security) {
    Security.OPEN -> "Open network: anyone nearby can read traffic that is not encrypted by the app or website itself."
    Security.WEP -> "WEP: an encryption broken years ago, which can be cracked in minutes."
    else -> "This network is not protected well."
}
