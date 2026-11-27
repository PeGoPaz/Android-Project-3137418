package tech.vladr.aegis.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.Surface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowSensor
import kotlin.math.cos
import kotlin.math.sin

/**
 * The compass against Robolectric's sensor manager, which runs Android's own
 * rotation maths on the JVM and lets a test play sensor readings.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CompassTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sensors = context.getSystemService(SensorManager::class.java)

    /** A phone lying flat, turned [degrees] anticlockwise from facing north, as a rotation vector. */
    private fun turnedAnticlockwise(degrees: Double): FloatArray {
        val half = Math.toRadians(degrees) / 2
        return floatArrayOf(0f, 0f, sin(half).toFloat(), cos(half).toFloat())
    }

    @Test
    fun flatAndFacingNorthIsNorth() {
        assertEquals(0f, azimuthOf(turnedAnticlockwise(0.0), Surface.ROTATION_0), 0.01f)
    }

    @Test
    fun turnedAnticlockwiseTheTopFacesWest() {
        assertEquals(270f, azimuthOf(turnedAnticlockwise(90.0), Surface.ROTATION_0), 0.01f)
    }

    @Test
    fun onItsSideTheHeadingFollowsTheScreenNotThePhone() {
        // Turned anticlockwise into landscape, the screen's top is the phone's right edge, still facing north.
        assertEquals(0f, azimuthOf(turnedAnticlockwise(90.0), Surface.ROTATION_90), 0.01f)
    }

    @Test
    fun accuracyCodesFromAndroid() {
        assertEquals(HeadingAccuracy.HIGH, accuracyOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        assertEquals(HeadingAccuracy.LOW, accuracyOf(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
        assertEquals(HeadingAccuracy.UNRELIABLE, accuracyOf(SensorManager.SENSOR_STATUS_UNRELIABLE))
        assertTrue(HeadingAccuracy.LOW.needsCalibration)
        assertFalse(HeadingAccuracy.MEDIUM.needsCalibration)
    }

    @Test
    fun withoutTheSensorThereAreNoHeadings() = runTest {
        assertEquals(emptyList<Heading>(), Compass(context).headings().toList())
    }

    @Test
    fun readingsBecomeHeadingsUntilTheScreenStopsListening() = runTest {
        val shadow = shadowOf(sensors)
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
        val received = mutableListOf<Heading>()
        val listening = launch(UnconfinedTestDispatcher(testScheduler)) {
            Compass(context).headings().collect { received += it }
        }

        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        shadow.listeners.single().onAccuracyChanged(sensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
        shadow.sendSensorEventToListeners(SensorEventBuilder.newBuilder(sensor, turnedAnticlockwise(90.0)).build())

        assertEquals(1, received.size)
        assertEquals(270f, received.single().degrees, 0.01f)
        assertEquals(HeadingAccuracy.HIGH, received.single().accuracy)

        listening.cancel()
        assertTrue("the sensor is still on", shadow.listeners.isEmpty())
    }
}
