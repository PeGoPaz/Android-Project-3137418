package tech.vladr.aegis.ui.find

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt
import tech.vladr.aegis.ble.Bearing
import tech.vladr.aegis.ble.Trend
import tech.vladr.aegis.sensors.Heading
import tech.vladr.aegis.sensors.turn
import tech.vladr.aegis.ui.theme.Home
import tech.vladr.aegis.ui.theme.Warning

/** Strongest a phone hears a device lying on it, and weakest it hears one at all, in dBm. */
private const val NEAREST = -40
private const val FURTHEST = -100

/** How full the meter is for a signal: empty at -100 dBm, full at -40 dBm. */
fun closeness(rssi: Int): Float = ((rssi - FURTHEST).toFloat() / (NEAREST - FURTHEST)).coerceIn(0f, 1f)

/**
 * Roughly how far, in words. Not in metres: walls, bodies and how the device
 * is held change the signal too much for a number to be honest.
 */
fun distanceLabel(rssi: Int?): String = when {
    rssi == null -> "Not heard"
    rssi >= -55 -> "Right next to you"
    rssi >= -67 -> "Close"
    rssi >= -75 -> "A few metres away"
    rssi >= -85 -> "Further away"
    else -> "Barely heard"
}

/** The nearest of the eight points of the compass: N, NE, E and so on. */
fun compassPoint(degrees: Float): String = POINTS[((degrees + 22.5f) / 45f).toInt() % POINTS.size]

private val POINTS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/**
 * A turn as the user would say it: [degrees] clockwise is to the right,
 * anticlockwise to the left. Within 15 degrees is straight ahead: the sweep
 * finds the device to within 30 degrees, so any closer would be made up.
 */
fun directionWords(degrees: Float): String {
    val size = abs(degrees).roundToInt()
    return when {
        size <= 15 -> "Straight ahead"
        size >= 165 -> "Behind you"
        degrees > 0 -> "$size° to your right"
        else -> "$size° to your left"
    }
}

@Composable
fun FindScreen(viewModel: FindViewModel, name: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FindScreen(state, name, onBack)
}

/** Finding a device by walking: a signal meter, and whether the last few steps made it warmer or colder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindScreen(state: FindUiState, name: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Find $name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.problem != null) {
                Text(state.problem, color = Warning, textAlign = TextAlign.Center)
                return@Column
            }
            Meter(state.level)
            TrendLine(state)
            // The arrow needs the compass twice: once in the sweep, and once now, to turn the bearing into "left" or "right".
            state.heading?.let { heading ->
                Direction(heading, state.bearing)
                HeadingLine(heading)
            }
            Text(
                "Hold the phone in front of you. Your body blocks the signal from behind, so it is strongest " +
                    "when you face the device. Walk the way of the arrow, and turn round again every few steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Three quarters of a ring, filling up as the signal gets stronger, with the level in the middle. */
@Composable
private fun Meter(level: Int?) {
    val track = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primary
    val label = distanceLabel(level)
    Box(
        Modifier.size(240.dp).semantics { contentDescription = level?.let { "$label, $it dBm" } ?: label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = stroke)
            if (level != null) drawArc(fill, startAngle = 135f, sweepAngle = 270f * closeness(level), useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(level?.toString() ?: "—", style = MaterialTheme.typography.displayMedium)
            Text("dBm", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun TrendLine(state: FindUiState) {
    val (text, colour) = when {
        state.silentFor == null -> "Listening..." to MaterialTheme.colorScheme.onSurfaceVariant
        state.level == null -> "Not heard for ${state.silentFor} s. Go back to where it was warmer." to Warning
        state.trend == Trend.WARMER -> "Warmer" to MaterialTheme.colorScheme.primary
        state.trend == Trend.COLDER -> "Colder" to Home
        state.trend == Trend.SAME -> "About the same" to MaterialTheme.colorScheme.onSurface
        else -> "Keep walking" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.headlineSmall, color = colour, textAlign = TextAlign.Center)
}

/** An arrow to where the signal was strongest, turned as the phone turns; or how to make one appear. */
@Composable
private fun Direction(heading: Heading, bearing: Bearing?) {
    if (bearing == null) {
        Text(
            "Turn slowly on the spot, all the way round, to see which way it is.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    val angle = turn(heading.degrees, bearing.degrees)
    val colour = MaterialTheme.colorScheme.primary
    val words = directionWords(angle)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .size(88.dp)
                .rotate(angle)
                .semantics { contentDescription = "Strongest signal: $words" },
        ) {
            // An arrowhead pointing up, the top of the screen; rotated, it points to the bearing.
            // Every corner lies inside the circle the box fits, so no turn pushes it onto the text below.
            val arrow = Path().apply {
                moveTo(size.width * 0.5f, size.height * 0.1f)
                lineTo(size.width * 0.8f, size.height * 0.85f)
                lineTo(size.width * 0.5f, size.height * 0.68f)
                lineTo(size.width * 0.2f, size.height * 0.85f)
                close()
            }
            drawPath(arrow, colour)
        }
        Text("Strongest signal: $words", style = MaterialTheme.typography.titleMedium)
        Text(
            "Towards ${compassPoint(bearing.degrees)}, ${bearing.contrastDb} dB stronger than other ways",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeadingLine(heading: Heading) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Facing ${compassPoint(heading.degrees)}, ${heading.degrees.roundToInt() % 360}°")
        if (heading.accuracy.needsCalibration) {
            Text(
                "The compass is unsure: move the phone in a figure of eight a few times.",
                style = MaterialTheme.typography.bodySmall,
                color = Warning,
                textAlign = TextAlign.Center,
            )
        }
    }
}
