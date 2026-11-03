package tech.vladr.aegis.ui.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import tech.vladr.aegis.ui.components.EmptyState
import tech.vladr.aegis.ui.components.ScreenTitle

/** Warnings about unsafe networks seen nearby. */
@Composable
fun AlertsScreen() {
    Column {
        ScreenTitle("Alerts")
        EmptyState("No alerts. Open and WEP networks you pass will be listed here.")
    }
}
