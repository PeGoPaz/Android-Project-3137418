package tech.vladr.aegis.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.ui.AegisHeader
import tech.vladr.aegis.ui.components.EmptyState
import tech.vladr.aegis.ui.components.statusColor

/** The home screen: your route and the networks along it, coloured by status. */
@Composable
fun MapScreen(onDeviceClick: (Long) -> Unit = {}, viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        AegisHeader()
        if (state.loaded && state.markers.isEmpty()) {
            EmptyState("Your route and the networks along it will appear here after your first scan with location on.")
        } else {
            Box(Modifier.fillMaxSize()) {
                MapLibreView(state, onDeviceClick, Modifier.fillMaxSize())
                Legend(Modifier.align(Alignment.TopStart).padding(12.dp))
                Text(
                    "${state.markers.size} networks · ${state.markers.count { it.unsafe }} unsafe",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** What each colour means, as in the mockup. */
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(DeviceStatus.TRUSTED to "Trusted", DeviceStatus.HOME to "Home", DeviceStatus.UNKNOWN to "Unknown", DeviceStatus.SUSPICIOUS to "Suspicious")
            .forEach { (status, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(statusColor(status), CircleShape))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
    }
}
