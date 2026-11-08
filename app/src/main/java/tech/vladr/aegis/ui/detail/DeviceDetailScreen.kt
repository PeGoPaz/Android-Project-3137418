package tech.vladr.aegis.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.data.Security
import tech.vladr.aegis.data.Sighting
import tech.vladr.aegis.scan.isUnsafe
import tech.vladr.aegis.ui.components.SignalBars
import tech.vladr.aegis.ui.shareText
import tech.vladr.aegis.ui.components.StatusTag
import tech.vladr.aegis.ui.components.Tag
import tech.vladr.aegis.ui.components.statusColor
import tech.vladr.aegis.ui.scan.securityLabel
import tech.vladr.aegis.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(viewModel: DeviceDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Deleted - from this page or elsewhere: nothing left to show.
    LaunchedEffect(state) { if (state.loaded && state.device == null) onBack() }
    val device = state.device ?: return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(device.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; renaming = true })
                        DropdownMenuItem(text = { Text("Clear history") }, onClick = { menuOpen = false; viewModel.clearHistory() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Summary(device, state) }
            item { StatusChooser(device.status, viewModel::setStatus) }
            item {
                OutlinedButton(
                    onClick = { shareText(context, "Aegis report: ${device.displayName}", deviceReport(device, state.sightings)) },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Share report")
                }
            }
            item {
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            if (state.sightings.isEmpty()) {
                item { Text("No sightings kept.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            }
            items(state.sightings, key = { it.id }) { sighting ->
                SightingRow(sighting)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }

    if (renaming) {
        RenameDialog(current = device.customName ?: device.name, onDone = { renaming = false; it?.let(viewModel::rename) })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${device.displayName}?") },
            text = { Text("Its whole history and its alerts go too. If it is seen again, it comes back as a new device.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Summary(device: Device, state: DeviceDetailUiState) {
    val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    val security = device.security ?: Security.UNKNOWN
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Tag(securityLabel(security).uppercase(), if (security.isUnsafe) Warning else MaterialTheme.colorScheme.primary)
            StatusTag(device.status)
        }
        if (device.customName != null && device.name.isNotEmpty()) Line("Network name", device.name)
        Line("BSSID", device.address.uppercase())
        Line("First seen", format.format(Date(device.firstSeen)))
        Line("Last seen", format.format(Date(device.lastSeen)))
        Line("Seen", times(state.sightings.size))
        state.strongest?.let { Line("Strongest signal", "$it dBm") }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value)
    }
}

/** Trusted, home, unknown or suspicious: one of four, chosen with a tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChooser(current: DeviceStatus, onChoose: (DeviceStatus) -> Unit) {
    val options = listOf(DeviceStatus.TRUSTED to "Trusted", DeviceStatus.HOME to "Home", DeviceStatus.UNKNOWN to "Unknown", DeviceStatus.SUSPICIOUS to "Suspicious")
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("This is...", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (status, label) ->
                SegmentedButton(
                    selected = current == status,
                    onClick = { onChoose(status) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = statusColor(status).copy(alpha = 0.25f)),
                    // No tick: the colour shows the choice, and four labels need the room.
                    icon = {},
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@Composable
private fun SightingRow(sighting: Sighting) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(sighting.timestamp)))
            Text(
                place(sighting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SignalBars(sighting.rssi)
        Text("${sighting.rssi} dBm", style = MaterialTheme.typography.bodySmall)
    }
}

fun place(sighting: Sighting): String =
    if (sighting.latitude == null || sighting.longitude == null) {
        "no location"
    } else {
        // Always with a decimal point: a comma, as some languages write it, would run into the separator.
        "%.5f, %.5f".format(Locale.ROOT, sighting.latitude, sighting.longitude) + (sighting.accuracy?.let { " ±${it.roundToInt()} m" } ?: "")
    }

@Composable
private fun RenameDialog(current: String, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Rename") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Name (empty for the network's own)") })
        },
        confirmButton = { TextButton(onClick = { onDone(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}
