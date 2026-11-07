package tech.vladr.aegis.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.ui.components.EmptyState
import tech.vladr.aegis.ui.components.StatusTag

/** Your own devices and networks: added by hand or marked on their page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedDevicesScreen(
    onBack: () -> Unit,
    onDeviceClick: (Long) -> Unit = {},
    viewModel: TrustedDevicesViewModel = viewModel(factory = TrustedDevicesViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Device?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Trusted devices") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Add a device")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val list = devices ?: return@Box
            if (list.isEmpty()) {
                EmptyState("No trusted devices yet. Add one with +, or mark a network as trusted or home on its page.")
            } else {
                LazyColumn {
                    items(list, key = { it.id }) { device ->
                        OwnDeviceRow(
                            device,
                            onOpen = { onDeviceClick(device.id) },
                            onRename = { renaming = device },
                            onForget = { viewModel.forget(device.id) },
                            onDelete = { viewModel.delete(device.id) },
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        AddDeviceDialog(onDone = { result ->
            adding = false
            result?.let { (address, name, status) -> viewModel.add(address, name, status) }
        })
    }
    renaming?.let { device ->
        NameDialog("Rename", device.customName ?: device.name, onDone = { name ->
            renaming = null
            name?.let { viewModel.rename(device.id, it) }
        })
    }
}

@Composable
private fun OwnDeviceRow(device: Device, onOpen: () -> Unit, onRename: () -> Unit, onForget: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(device.displayName) },
        supportingContent = { Text(device.address.uppercase()) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusTag(device.status)
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
                        DropdownMenuItem(text = { Text("Not mine") }, onClick = { menuOpen = false; onForget() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onOpen),
    )
}

/** Asks for an address, a name and whether it is a trusted device or a home one. */
@Composable
private fun AddDeviceDialog(onDone: (Triple<String, String, DeviceStatus>?) -> Unit) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(DeviceStatus.TRUSTED) }
    val valid = isMacAddress(address)
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Add a device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("MAC address or BSSID") },
                    placeholder = { Text("AC:F8:CC:05:0F:A8") },
                    isError = address.isNotEmpty() && !valid,
                    supportingText = { if (address.isNotEmpty() && !valid) Text("Six pairs of hex digits, like AC:F8:CC:05:0F:A8") },
                    singleLine = true,
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = status == DeviceStatus.TRUSTED, onClick = { status = DeviceStatus.TRUSTED }, label = { Text("Trusted") })
                    FilterChip(selected = status == DeviceStatus.HOME, onClick = { status = DeviceStatus.HOME }, label = { Text("Home") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(Triple(address, name, status)) }, enabled = valid) { Text("Add") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(title: String, current: String, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onDone(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}
