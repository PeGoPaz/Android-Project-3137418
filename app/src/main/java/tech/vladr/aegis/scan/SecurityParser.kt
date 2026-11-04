package tech.vladr.aegis.scan

import tech.vladr.aegis.data.Security

/**
 * The protection of a network, read from the capabilities Android reports
 * for it: a string like "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]", one bracket
 * per thing the access point supports.
 *
 * The strongest protection offered counts: a network offering WPA2 and
 * WPA3 together (transition mode) is reported as WPA3.
 */
fun securityOf(capabilities: String): Security {
    val caps = capabilities.uppercase()
    return when {
        // SAE is WPA3-Personal; Suite B 192-bit is WPA3-Enterprise.
        "SAE" in caps || "SUITE-B-192" in caps -> Security.WPA3
        // Opportunistic Wireless Encryption: no password, but encrypted.
        "OWE" in caps -> Security.OWE
        // RSN is the IEEE name of WPA2.
        "RSN" in caps || "WPA2" in caps -> Security.WPA2
        "WPA" in caps -> Security.WPA
        "WEP" in caps -> Security.WEP
        else -> Security.OPEN
    }
}

/** Networks a stranger can read or join without effort. */
val Security.isUnsafe: Boolean
    get() = this == Security.OPEN || this == Security.WEP
