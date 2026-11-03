package tech.vladr.aegis.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.vladr.aegis.ui.components.ScreenTitle

/** How Aegis scans, and the way to the list of your own devices. */
@Composable
fun SettingsScreen(onTrustedDevices: () -> Unit) {
    Column {
        ScreenTitle("Settings")
        ListItem(
            headlineContent = { Text("Trusted devices") },
            supportingContent = { Text("Your own devices and networks, which never trigger alerts") },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.clickable(onClick = onTrustedDevices).padding(horizontal = 4.dp),
        )
    }
}
