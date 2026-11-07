package tech.vladr.aegis.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.vladr.aegis.BuildConfig
import tech.vladr.aegis.ui.components.ScreenTitle
import tech.vladr.aegis.ui.theme.Suspicious

/** How Aegis scans, the way to your own devices, and wiping what it saved. */
@Composable
fun SettingsScreen(onTrustedDevices: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenTitle("Settings")
        Entry("Trusted devices", "Your own devices and networks, which never trigger alerts", onClick = onTrustedDevices)
        Divider()
        Toggle("Show hidden networks", "List networks that do not broadcast their name", settings.showHiddenNetworks, viewModel::setShowHiddenNetworks)
        Toggle(
            "Save scans without a location",
            "Keep a scan even when the phone had no GPS fix; it will not be on the map",
            settings.saveWithoutLocation,
            viewModel::setSaveWithoutLocation,
        )
        Toggle("Unsafe network alerts", "Warn about open and WEP networks", settings.unsafeNetworkAlerts, viewModel::setUnsafeNetworkAlerts)
        Divider()
        Entry("Delete everything", "Every network, sighting and alert Aegis has saved", color = Suspicious, onClick = { confirmDelete = true })
        Divider()
        Entry("About", "Aegis ${BuildConfig.VERSION_NAME}. Listens only; everything stays on this phone.")
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete everything?") },
            text = { Text("All networks, their history, your trusted devices and alerts are deleted from this phone. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteEverything() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Entry(title: String, detail: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground, onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = { Text(detail) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
private fun Toggle(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
