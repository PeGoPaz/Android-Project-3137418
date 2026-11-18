package tech.vladr.aegis.scan

/**
 * Whether a MAC address was made up by the device instead of given to it by
 * its maker.
 *
 * The second-lowest bit of the first byte says which: 0 for an address from
 * the block a maker buys (its OUI), 1 for a "locally administered" one. Phones
 * set it on the addresses they make up - the private Wi-Fi address of Android
 * and iOS, and the hotspot a phone shares - and some routers on the extra
 * networks they run beside their main one, such as a guest network.
 *
 * An address like that tells nothing about who made the device, and it can
 * change: the same phone may come back under a new one. The lowest bit marks
 * a group (multicast) address, which no network has, so it is not counted.
 */
fun isPrivateAddress(address: String): Boolean {
    val first = address.trim().take(2).toIntOrNull(16) ?: return false
    return first and LOCAL != 0 && first and GROUP == 0
}

private const val LOCAL = 0x02
private const val GROUP = 0x01
