package tech.vladr.aegis.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.scan.WifiNetwork
import tech.vladr.aegis.scan.isUnsafe
import tech.vladr.aegis.ui.components.EmptyState
import tech.vladr.aegis.ui.components.ScreenTitle
import tech.vladr.aegis.ui.components.SignalBars
import tech.vladr.aegis.ui.components.StatusTag
import tech.vladr.aegis.ui.components.Tag
import tech.vladr.aegis.ui.theme.Suspicious
import tech.vladr.aegis.ui.theme.Warning

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Networks from the latest scan, strongest signal first, and the button that scans. */
@Composable
fun ScanScreen(onDeviceClick: (Long) -> Unit = {}, viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var problem by remember { mutableStateOf<String?>(null) }

    // Scans only when Android will return networks: with the permission, and with location on.
    fun scanIfAllowed(askIfNeeded: () -> Unit) {
        when {
            !hasLocationPermission(context) -> askIfNeeded()
            !isLocationOn(context) -> problem = LOCATION_OFF
            else -> {
                problem = null
                viewModel.scan()
            }
        }
    }

    // Asks for the location permission; Android shows its own dialog and tells us the answer.
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        scanIfAllowed(askIfNeeded = { problem = NO_PERMISSION })
    }

    fun startScan() = scanIfAllowed(askIfNeeded = { askPermission.launch(LOCATION_PERMISSIONS) })

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "Live Scan",
            subtitle = if (state.lastScanAt == null) "No scan yet" else "${state.networks.size} networks nearby",
        )
        state.lastScanAt?.let { at ->
            val place = state.fix?.let { fix -> fix.accuracyMetres?.let { "location ±${it.roundToInt()} m" } ?: "location found" }
                ?: "no location fix, saved without a place"
            InfoLine("Last scan at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(at))} · $place")
        }
        (problem ?: state.notice)?.let { message ->
            InfoLine(message, color = Warning)
            if (problem == LOCATION_OFF) {
                TextButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) { Text("Turn on location") }
            }
        }
        Box(Modifier.weight(1f)) {
            if (state.networks.isEmpty()) {
                EmptyState(if (state.lastScanAt == null) "Press Scan to see the Wi-Fi networks around you." else "No networks found.")
            } else {
                LazyColumn {
                    items(state.networks, key = { it.bssid }) { network ->
                        val device = state.known[network.bssid]
                        NetworkRow(
                            network,
                            status = device?.status,
                            // Saved networks open their page; one not saved (an old result) has none yet.
                            modifier = if (device != null) Modifier.clickable { onDeviceClick(device.id) } else Modifier,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
        Button(
            onClick = ::startScan,
            enabled = !state.scanning,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (state.scanning) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Search, contentDescription = null)
            }
            Text(if (state.scanning) "  Scanning..." else "  Scan", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InfoLine(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}

/** One network: its name, address and protection, and how strong its signal is. */
@Composable
fun NetworkRow(network: WifiNetwork, modifier: Modifier = Modifier, status: DeviceStatus? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (network.security.isUnsafe) Icons.Filled.Warning else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (network.security.isUnsafe) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    network.ssid.ifEmpty { "Hidden network" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                StatusTag(status)
                when (network.security) {
                    Security.OPEN -> Tag("OPEN", Warning)
                    Security.WEP -> Tag("WEP", Suspicious)
                    else -> Unit
                }
            }
            Text(
                "${network.bssid.uppercase()} · ${securityLabel(network.security)} · ${band(network.frequencyMhz)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            SignalBars(network.rssi)
            Text("${network.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun securityLabel(security: Security): String = when (security) {
    Security.OPEN -> "open"
    Security.OWE -> "Enhanced Open"
    Security.UNKNOWN -> "unknown"
    else -> security.name
}

private fun band(frequencyMhz: Int): String = when {
    frequencyMhz >= 5925 -> "6 GHz"
    frequencyMhz >= 5000 -> "5 GHz"
    else -> "2.4 GHz"
}

private const val LOCATION_OFF = "Location is off. Android returns no Wi-Fi networks while it is."
private const val NO_PERMISSION = "Aegis needs the precise location permission: Android shows Wi-Fi networks only to apps that have it."

private fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun isLocationOn(context: Context) =
    context.getSystemService(LocationManager::class.java).isLocationEnabled
