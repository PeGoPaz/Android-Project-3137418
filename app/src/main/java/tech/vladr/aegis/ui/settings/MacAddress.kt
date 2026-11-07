package tech.vladr.aegis.ui.settings

/** A MAC address as six pairs of hex digits, with colons or dashes: AC:F8:CC:05:0F:A8. */
fun isMacAddress(text: String): Boolean = Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$").matches(text.trim())

/** The form Aegis stores addresses in, the same as Android reports BSSIDs: lower case, colons. */
fun normaliseMac(text: String): String = text.trim().replace('-', ':').lowercase()
