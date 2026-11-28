package tech.vladr.aegis.detection

/**
 * What it takes for a device to count as following you: seen in [places]
 * places at least [placeRadiusMetres] apart, over at least [minSpanMs].
 */
data class FollowingThresholds(val places: Int, val placeRadiusMetres: Double, val minSpanMs: Long)

/**
 * How readily the following check alerts, chosen in Settings. Every level is
 * a trade: alerting sooner also alerts more often for what travels with you
 * anyway - the bus's Wi-Fi, a friend's earbuds.
 */
enum class Sensitivity(val label: String, val explanation: String, val thresholds: FollowingThresholds) {
    HIGH(
        "High",
        "Alerts after two places 150 m apart. Sooner, but a bus or a friend's phone can set it off.",
        FollowingThresholds(places = 2, placeRadiusMetres = 150.0, minSpanMs = 5 * MINUTE),
    ),
    NORMAL(
        "Normal",
        "Alerts after three places 200 m apart, over at least ten minutes.",
        FollowingThresholds(places = 3, placeRadiusMetres = 200.0, minSpanMs = 10 * MINUTE),
    ),
    LOW(
        "Low",
        "Alerts after four places 300 m apart, over at least half an hour. Fewer false alarms, later.",
        FollowingThresholds(places = 4, placeRadiusMetres = 300.0, minSpanMs = 30 * MINUTE),
    ),
}

private const val MINUTE = 60_000L
