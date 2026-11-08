package tech.vladr.aegis.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date
import tech.vladr.aegis.data.AlertWithDevice
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.detection.unsafeReason
import tech.vladr.aegis.ui.components.EmptyState
import tech.vladr.aegis.ui.openWifiSettings
import tech.vladr.aegis.ui.components.ScreenTitle
import tech.vladr.aegis.ui.components.Tag
import tech.vladr.aegis.ui.theme.Suspicious
import tech.vladr.aegis.ui.theme.Warning

/** Warnings about unsafe networks seen nearby, newest first. */
@Composable
fun AlertsScreen(onDeviceClick: (Long) -> Unit = {}, viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory)) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Alerts", subtitle = alerts?.let { if (it.isEmpty()) "Nothing to worry about" else "${it.size} to look at" })
        val list = alerts ?: return@Column
        if (list.isEmpty()) {
            EmptyState("No alerts. Open and WEP networks you pass will be listed here, so you know not to use them.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                items(list, key = { it.alert.id }) { alert ->
                    AlertCard(alert, onOpen = { onDeviceClick(alert.alert.deviceId) }, onDismiss = { viewModel.dismiss(alert.alert.id) })
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertWithDevice, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val color = if (alert.deviceSecurity == Security.WEP) Suspicious else Warning
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = color)
            }
            Column(Modifier.weight(1f)) {
                Text(alert.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${alert.deviceAddress.uppercase()} · seen ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(alert.alert.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Tag(if (alert.deviceSecurity == Security.WEP) "WEP" else "OPEN", color)
        }
        Text(unsafeReason(alert.deviceSecurity), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // If you are on this network now, Android's Wi-Fi settings are where you leave it.
            TextButton(onClick = { openWifiSettings(context) }) { Text("Wi-Fi settings") }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
