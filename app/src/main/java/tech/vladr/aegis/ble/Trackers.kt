package tech.vladr.aegis.ble

/**
 * What a Bluetooth LE advertisement says about the device that sent it, cut
 * down to what tells a tracker apart. The scanner fills it from Android's
 * ScanRecord; keeping it free of Android types lets the rules below be tested
 * on the JVM.
 *
 * Keys are the 16-bit numbers Bluetooth SIG assigns: company ids for
 * manufacturer data (0x004C is Apple), service UUIDs (0xFEED is Tile) for the
 * rest.
 */
class Advertisement(
    /** Manufacturer specific data by company id, without the id itself. */
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    /** Service data by 16-bit service UUID, without the UUID. */
    val serviceData: Map<Int, ByteArray> = emptyMap(),
    /** The 16-bit service UUIDs the advertisement lists. */
    val serviceUuids: Set<Int> = emptySet(),
)

enum class TrackerKind(val label: String) {
    AIRTAG("AirTag"),
    FIND_MY_ACCESSORY("Find My accessory"),
    AIRPODS("AirPods"),
    APPLE_DEVICE("Apple device"),
    GOOGLE_FIND_MY("Find My Device tracker"),
    SMARTTAG("Samsung SmartTag"),
    TILE("Tile"),
    CHIPOLO("Chipolo"),
    PEBBLEBEE("Pebblebee"),
}

/**
 * A device on a finding network: anyone's phone nearby reports where it is
 * to its owner. That is what makes a small one useful for following a person.
 *
 * [separated] is whether it is away from its owner's phone, which is when it
 * would be travelling with someone else; null where the advertisement does
 * not say.
 */
data class Tracker(val kind: TrackerKind, val separated: Boolean? = null)

/**
 * Which tracker sent an advertisement, if any.
 *
 * The values come from the networks' own formats, as documented by AirGuard
 * (SEEMOO, TU Darmstadt) and Google's Find My Device Network specification.
 */
fun recogniseTracker(ad: Advertisement): Tracker? =
    apple(ad.manufacturerData[APPLE])
        ?: google(ad.serviceData[EDDYSTONE])
        ?: when {
            SAMSUNG_TAG in ad.serviceData || SAMSUNG_TAG in ad.serviceUuids -> Tracker(TrackerKind.SMARTTAG)
            TILE in ad.serviceUuids || TILE in ad.serviceData -> Tracker(TrackerKind.TILE)
            CHIPOLO in ad.serviceUuids || CHIPOLO in ad.serviceData -> Tracker(TrackerKind.CHIPOLO)
            PEBBLEBEE in ad.serviceUuids || PEBBLEBEE in ad.serviceData -> Tracker(TrackerKind.PEBBLEBEE)
            else -> null
        }

/**
 * Apple's Find My ("offline finding") message: type 0x12, its length, then a
 * status byte. The length is 25 (0x19) while the device is away from its
 * owner and 2 while it is near; bits 4-5 of the status byte say what it is.
 * Other Apple messages (AirDrop, Handoff, ...) are not about finding.
 */
private fun apple(data: ByteArray?): Tracker? {
    if (data == null || data.size < 3 || data[0] != OFFLINE_FINDING) return null
    val kind = when ((data[2].toInt() and 0x30) shr 4) {
        1 -> TrackerKind.AIRTAG
        2 -> TrackerKind.FIND_MY_ACCESSORY
        3 -> TrackerKind.AIRPODS
        else -> TrackerKind.APPLE_DEVICE
    }
    return Tracker(kind, separated = data[1] == SEPARATED_LENGTH)
}

/**
 * Google's Find My Device Network uses the Eddystone service with frame type
 * 0x40, or 0x41 once the tracker has been away from its owner long enough
 * ("unwanted tracking protection" mode). Plain Eddystone beacons - URL, UID,
 * TLM and EID frames - share the service and are not trackers.
 */
private fun google(data: ByteArray?): Tracker? {
    val frame = data?.firstOrNull()?.toInt()?.and(0xFF) ?: return null
    return when (frame) {
        0x40 -> Tracker(TrackerKind.GOOGLE_FIND_MY, separated = false)
        0x41 -> Tracker(TrackerKind.GOOGLE_FIND_MY, separated = true)
        else -> null
    }
}

private const val APPLE = 0x004C
private const val OFFLINE_FINDING: Byte = 0x12
private const val SEPARATED_LENGTH: Byte = 0x19
private const val EDDYSTONE = 0xFEAA
private const val SAMSUNG_TAG = 0xFD5A
private const val TILE = 0xFEED
private const val CHIPOLO = 0xFE33
private const val PEBBLEBEE = 0xFA25
