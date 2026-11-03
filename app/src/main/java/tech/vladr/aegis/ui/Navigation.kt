package tech.vladr.aegis.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** The four screens of the bottom bar, in the order the mockups show them. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Map("map", "Map", Icons.Filled.Place),
    Scan("scan", "Scan", Icons.Filled.Search),
    Alerts("alerts", "Alerts", Icons.Filled.Notifications),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

/** Screens reached from a tab rather than from the bottom bar. */
object Routes {
    const val TRUSTED = "trusted"
}
