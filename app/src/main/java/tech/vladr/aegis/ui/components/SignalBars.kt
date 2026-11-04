package tech.vladr.aegis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** How many of four bars a signal fills: four from -55 dBm up, none below -85 dBm. */
fun signalLevel(rssi: Int): Int = when {
    rssi >= -55 -> 4
    rssi >= -67 -> 3
    rssi >= -75 -> 2
    rssi >= -85 -> 1
    else -> 0
}

/** Four bars of rising height, as phones show signal strength. */
@Composable
fun SignalBars(rssi: Int, modifier: Modifier = Modifier) {
    val level = signalLevel(rssi)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (bar in 1..4) {
            Box(
                Modifier
                    .width(4.dp)
                    .height((4 + bar * 4).dp)
                    .background(
                        if (bar <= level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}
