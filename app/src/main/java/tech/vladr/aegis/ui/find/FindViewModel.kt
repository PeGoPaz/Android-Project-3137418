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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.ble.Bearing
import tech.vladr.aegis.ble.Reading
import tech.vladr.aegis.ble.Sample
import tech.vladr.aegis.ble.SignalFollower
import tech.vladr.aegis.ble.Trend
import tech.vladr.aegis.ble.bearingOf
import tech.vladr.aegis.ble.keepRecent
import tech.vladr.aegis.ble.keepSweep
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
    /** Which way the signal was strongest while turning on the spot; null until a sweep shows it. */
    val bearing: Bearing? = null,
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
    /** Something that changes the search: an advertisement heard, the phone turned, or a second gone by. */
    private sealed interface Event {
        data class Heard(val reading: Reading) : Event
        data class Facing(val heading: Heading?) : Event
        data object Tick : Event
    }

    /**
     * Everything heard so far: the readings still in use, the time of the last
     * one, which way the phone faces, and the sweep - every reading paired with
     * the heading it was taken at.
     */
    private data class Search(
        val readings: List<Reading> = emptyList(),
        val last: Long? = null,
        val heading: Heading? = null,
        val sweep: List<Sample> = emptyList(),
    ) {
        fun after(event: Event, now: Long): Search = when (event) {
            is Event.Heard -> copy(
                readings = keepRecent(readings + event.reading, now),
                last = event.reading.at,
                sweep = keepSweep(sweep + listOfNotNull(heading?.let { Sample(it.degrees, event.reading.rssi, event.reading.at) }), now),
            )
            is Event.Facing -> copy(heading = event.heading)
            Event.Tick -> copy(readings = keepRecent(readings, now), sweep = keepSweep(sweep, now))
        }

        fun toUiState(now: Long) = FindUiState(
            level = levelAt(readings, now),
            trend = trendAt(readings, now),
            silentFor = last?.let { (now - it) / 1_000 },
            heading = heading,
            bearing = bearingOf(sweep),
        )
    }

    val state: StateFlow<FindUiState> =
        merge(
            follower.follow(address).map { Event.Heard(it) },
            compass.headings().map { Event.Facing(it) },
            // A tick every second, so that silence shows too.
            ticks(),
        )
            .runningFold(Search()) { search, event -> search.after(event, clock()) }
            .map { it.toUiState(clock()) }
            .catch { emit(FindUiState(problem = problemOf(it))) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FindUiState())

    private fun ticks(): Flow<Event> = flow {
        while (true) {
            emit(Event.Tick)
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
