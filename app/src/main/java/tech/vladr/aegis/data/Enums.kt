package tech.vladr.aegis.data

/** What kind of radio a device was seen through. */
enum class DeviceKind { WIFI_AP, BLE }

/**
 * How a Wi-Fi network protects its traffic, from none to the strongest.
 * OWE ("Enhanced Open") has no password but is still encrypted.
 */
enum class Security { OPEN, OWE, WEP, WPA, WPA2, WPA3, UNKNOWN }

/** What you told Aegis about a device. Trusted and home devices never cause alerts. */
enum class DeviceStatus { TRUSTED, HOME, UNKNOWN, SUSPICIOUS }

/** Why an alert was raised: an unsafe network now, a device following you from v2. */
enum class AlertKind { UNSAFE_NETWORK, FOLLOWING }
