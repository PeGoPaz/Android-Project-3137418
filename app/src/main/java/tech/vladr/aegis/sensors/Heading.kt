package tech.vladr.aegis.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow

/**
 * Which way the phone faces: [degrees] clockwise from magnetic north, 0 to 360,
 * measured along the top edge of the screen as the user holds it.
 */
data class Heading(val degrees: Float, val accuracy: HeadingAccuracy)

/** How far to trust a heading. Android reports it for the magnetometer behind it. */
enum class HeadingAccuracy {
    UNRELIABLE, LOW, MEDIUM, HIGH;

    /** Worth asking the user to wave the phone in a figure of eight, which recalibrates it. */
    val needsCalibration: Boolean get() = this <= LOW
}

/** Android's sensor status codes, as a [HeadingAccuracy]. */
fun accuracyOf(status: Int): HeadingAccuracy = when (status) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HeadingAccuracy.HIGH
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> HeadingAccuracy.MEDIUM
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> HeadingAccuracy.LOW
    else -> HeadingAccuracy.UNRELIABLE
}

/** Something that says which way the phone faces: the compass, or a fixed direction in tests. */
fun interface HeadingSource {
    fun headings(): Flow<Heading>
}

/**
 * The phone's heading from Android's rotation vector sensor. It is not one
 * chip but a fusion of three: the magnetometer finds north, the accelerometer
 * finds down, and the gyroscope smooths out quick turns, which a magnetometer
 * alone shows late and shakily.
 *
 * Collecting [headings] starts the sensor and cancelling the collection stops
 * it, so the compass only uses the battery while a screen shows it. A phone
 * without the sensor gives no headings at all.
 *
 * @param rotation how the screen is turned now, a Surface.ROTATION_* value,
 *   asked on every reading; the app reads it from the phone's display.
 *   Without it the compass assumes portrait.
 */
class Compass(
    context: Context,
    private val rotation: () -> Int = { Surface.ROTATION_0 },
) : HeadingSource {
    private val sensors = context.getSystemService(SensorManager::class.java)

    override fun headings(): Flow<Heading> {
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return emptyFlow()
        return callbackFlow {
            var smoothed: Float? = null
            var accuracy = HeadingAccuracy.UNRELIABLE
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val degrees = smooth(smoothed, azimuthOf(event.values, rotation()))
                    smoothed = degrees
                    trySend(Heading(degrees, accuracy))
                }

                override fun onAccuracyChanged(sensor: Sensor, status: Int) {
                    accuracy = accuracyOf(status)
                }
            }
            sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sensors.unregisterListener(listener) }
        }.conflate() // a slow screen gets the newest heading, not a queue of old ones
    }
}

/**
 * The azimuth, in degrees from north, of the top edge of the screen, from a
 * rotation vector (the phone's orientation as a quaternion). Turned on its
 * side, the top of the screen is a different edge of the phone, so the axes
 * are swapped to match before the angle is read.
 */
fun azimuthOf(rotationVector: FloatArray, screenRotation: Int): Float {
    val matrix = FloatArray(9)
    SensorManager.getRotationMatrixFromVector(matrix, rotationVector)
    val (x, y) = when (screenRotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
    val remapped = FloatArray(9)
    SensorManager.remapCoordinateSystem(matrix, x, y, remapped)
    val orientation = FloatArray(3)
    SensorManager.getOrientation(remapped, orientation)
    return normalise(Math.toDegrees(orientation[0].toDouble()).toFloat())
}
