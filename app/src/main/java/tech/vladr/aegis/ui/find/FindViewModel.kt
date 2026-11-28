package tech.vladr.aegis.ui.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.ble.Reading
import tech.vladr.aegis.ble.SignalFollower
import tech.vladr.aegis.ble.Trend
import tech.vladr.aegis.ble.keepRecent
import tech.vladr.aegis.ble.levelAt
import tech.vladr.aegis.ble.trendAt
import tech.vladr.aegis.sensors.Heading
import tech.vladr.aegis.sensors.HeadingSource

data class FindUiState(
    /** The signal now, in dBm, over the last few seconds; null when it has not been heard lately. */
    val level: Int? = null,
    /** Warmer or colder than a few seconds ago; null until there is enough to say. */
    val trend: Trend? = null,
    /** How long since the device was last heard, in seconds; null before the first time. */
    val silentFor: Long? = null,
    /** Which way the phone faces; null on a phone without a compass. */
    val heading: Heading? = null,
    /** Why the search cannot run, for the user: no permission, Bluetooth off. */
    val problem: String? = null,
)

/**
 * The search for one Bluetooth device. It listens to that device's signal and
 * to the compass only while the screen shows it: both flows start when the
 * screen starts collecting [state], and stop five seconds after it stops (the
 * five seconds ride out a screen rotation without restarting the scan).
 */
class FindViewModel(
    address: String,
    follower: SignalFollower,
    compass: HeadingSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    /** What has been heard so far: the readings still in use, and the time of the last one. */
    private data class Heard(val readings: List<Reading> = emptyList(), val last: Long? = null)

    private val signal: Flow<FindUiState> =
        // A reading when the device advertises, and a tick every second, so that silence shows too.
        merge(follower.follow(address), ticks())
            .runningFold(Heard()) { heard, reading ->
                Heard(keepRecent(heard.readings + listOfNotNull(reading), clock()), reading?.at ?: heard.last)
            }
            .map { heard ->
                val now = clock()
                FindUiState(
                    level = levelAt(heard.readings, now),
                    trend = trendAt(heard.readings, now),
                    silentFor = heard.last?.let { (now - it) / 1_000 },
                )
            }
            .catch { emit(FindUiState(problem = problemOf(it))) }

    val state: StateFlow<FindUiState> =
        combine(signal, compass.headings().map<Heading, Heading?> { it }.onStart { emit(null) }) { signal, heading ->
            signal.copy(heading = heading)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FindUiState())

    private fun ticks(): Flow<Reading?> = flow {
        while (true) {
            emit(null)
            delay(TICK_MS)
        }
    }

    companion object {
        const val TICK_MS = 1_000L

        const val NO_PERMISSION = "Aegis needs the nearby devices permission to listen for it. Allow it from Live Scan."
        const val BLUETOOTH_OFF = "Bluetooth is off. Turn it on to listen for the device."

        fun problemOf(error: Throwable): String = when (error) {
            is SecurityException -> NO_PERMISSION
            else -> error.message?.takeIf { "refused" in it }?.let { "$it. Try again in a minute." } ?: BLUETOOTH_OFF
        }

        fun factory(address: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AegisApplication
                FindViewModel(address, app.bluetooth, app.compass)
            }
        }
    }
}
