package tech.vladr.aegis.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import tech.vladr.aegis.ui.AegisHeader
import tech.vladr.aegis.ui.components.EmptyState

/** The home screen: your route and the networks along it. */
@Composable
fun MapScreen() {
    Column {
        AegisHeader()
        EmptyState("Your route and the networks along it will appear here after your first scan.")
    }
}
