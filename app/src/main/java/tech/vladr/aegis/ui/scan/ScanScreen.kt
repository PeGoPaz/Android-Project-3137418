package tech.vladr.aegis.ui.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import tech.vladr.aegis.ui.components.EmptyState
import tech.vladr.aegis.ui.components.ScreenTitle

/** Networks from the latest scan, strongest signal first. */
@Composable
fun ScanScreen() {
    Column {
        ScreenTitle("Live Scan", "No scan yet")
        EmptyState("Press Scan to see the Wi-Fi networks around you.")
    }
}
