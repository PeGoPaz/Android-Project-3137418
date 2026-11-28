package tech.vladr.aegis.ui.find

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.vladr.aegis.ble.Reading
import tech.vladr.aegis.ble.SignalFollower
import tech.vladr.aegis.ble.Trend
import tech.vladr.aegis.sensors.Heading
import tech.vladr.aegis.sensors.HeadingAccuracy
import tech.vladr.aegis.sensors.HeadingSource

/** The search with a fake radio and compass, on the test clock: seconds pass without waiting for them. */
@OptIn(ExperimentalCoroutinesApi::class)
class FindViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun reset() = Dispatchers.resetMain()

    /** A radio the test speaks for, which counts how many searches are listening to it. */
    private class FakeRadio : SignalFollower {
        val heard = MutableSharedFlow<Reading>()
        var listening = 0
        override fun follow(address: String): Flow<Reading> = heard.onStart { listening++ }.onCompletion { listening-- }
    }

    private val radio = FakeRadio()
    private val southWest = HeadingSource { flowOf(Heading(225f, HeadingAccuracy.HIGH)) }

    private fun viewModel(follower: SignalFollower = radio, compass: HeadingSource = southWest) =
        FindViewModel("da:a1:19:7c:30:02", follower, compass, clock = { dispatcher.scheduler.currentTime })

    /** Hears one advertisement a second at these levels. */
    private suspend fun TestScope.walk(vararg rssi: Int) {
        for (level in rssi) {
            radio.heard.emit(Reading(level, testScheduler.currentTime))
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
        }
    }

    @Test
    fun walkingTowardsItIsWarmer() = runTest(dispatcher) {
        val search = viewModel()
        backgroundScope.launch { search.state.collect {} }
        runCurrent()

        walk(-88, -87, -89, -88, -86, -80, -78, -76, -75, -74)

        assertEquals(Trend.WARMER, search.state.value.trend)
        assertEquals(-75, search.state.value.level)
        assertEquals(1L, search.state.value.silentFor)
    }

    @Test
    fun walkingAwayIsColder() = runTest(dispatcher) {
        val search = viewModel()
        backgroundScope.launch { search.state.collect {} }
        runCurrent()

        walk(-62, -63, -61, -62, -64, -70, -74, -77, -79, -80)

        assertEquals(Trend.COLDER, search.state.value.trend)
    }

    @Test
    fun silenceShowsEvenWithNothingHeard() = runTest(dispatcher) {
        val search = viewModel()
        backgroundScope.launch { search.state.collect {} }
        runCurrent()

        walk(-70)
        advanceTimeBy(5_000)
        runCurrent()

        assertNull(search.state.value.level)
        assertEquals(6L, search.state.value.silentFor)
    }

    @Test
    fun theHeadingComesAlong() = runTest(dispatcher) {
        val search = viewModel()
        backgroundScope.launch { search.state.collect {} }
        runCurrent()

        assertEquals(225f, search.state.value.heading!!.degrees)
    }

    @Test
    fun turningOnTheSpotPointsToTheDevice() = runTest(dispatcher) {
        val facing = MutableStateFlow(Heading(0f, HeadingAccuracy.HIGH))
        val search = viewModel(compass = HeadingSource { facing })
        backgroundScope.launch { search.state.collect {} }
        runCurrent()
        assertNull(search.state.value.bearing)

        // A full turn in twelve seconds, the device to the east: the body blocks it facing any other way.
        for (degrees in 0 until 360 step 15) {
            facing.value = Heading(degrees.toFloat(), HeadingAccuracy.HIGH)
            runCurrent()
            radio.heard.emit(Reading(if (degrees in 90 until 120) -60 else -75, testScheduler.currentTime))
            runCurrent()
            advanceTimeBy(500)
        }

        val bearing = search.state.value.bearing
        assertNotNull(bearing)
        assertEquals(105f, bearing!!.degrees)
        assertEquals(15, bearing.contrastDb)
    }

    @Test
    fun withoutACompassTheSignalStillShows() = runTest(dispatcher) {
        val search = viewModel(compass = HeadingSource { emptyFlow() })
        backgroundScope.launch { search.state.collect {} }
        runCurrent()

        walk(-70)

        assertEquals(-70, search.state.value.level)
        assertNull(search.state.value.heading)
    }

    @Test
    fun withoutThePermissionItSaysSo() = runTest(dispatcher) {
        val search = viewModel(follower = SignalFollower { flow { throw SecurityException("no") } })
        backgroundScope.launch { search.state.collect {} }
        runCurrent()

        assertEquals(FindViewModel.NO_PERMISSION, search.state.value.problem)
    }

    @Test
    fun bluetoothOffAndARefusedScanSayWhatHappened() {
        assertEquals(FindViewModel.BLUETOOTH_OFF, FindViewModel.problemOf(IllegalStateException("Bluetooth is off")))
        assertEquals(
            "Android refused the Bluetooth scan (error 2). Try again in a minute.",
            FindViewModel.problemOf(IllegalStateException("Android refused the Bluetooth scan (error 2)")),
        )
    }

    @Test
    fun theScanStopsSoonAfterTheScreenGoes() = runTest(dispatcher) {
        val search = viewModel()
        val screen = launch { search.state.collect {} }
        runCurrent()
        assertEquals(1, radio.listening)

        screen.cancel()
        // Long enough for a rotation, which brings the screen straight back.
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(1, radio.listening)

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(0, radio.listening)
    }
}
