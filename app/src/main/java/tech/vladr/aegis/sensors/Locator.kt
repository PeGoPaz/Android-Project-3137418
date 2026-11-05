package tech.vladr.aegis.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Where the phone is, and how sure it is: the true spot is within [accuracyMetres] of it. */
data class Fix(val latitude: Double, val longitude: Double, val accuracyMetres: Float?)

/**
 * The phone's current location from Android's own LocationManager, so Aegis
 * runs without Google Play Services. GPS first; when it has no fix in time,
 * indoors for instance, the network location; and as a last resort the last
 * known location, if it is recent.
 */
class Locator(private val context: Context, private val clock: () -> Long = System::currentTimeMillis) {
    private val locations = context.getSystemService(LocationManager::class.java)

    /** Null without the permission, with location off, or when no provider gives a fix in time. */
    suspend fun current(): Fix? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val location = PROVIDERS
            .filter { locations.isProviderEnabled(it) }
            .firstNotNullOfOrNull { provider -> withTimeoutOrNull(TIMEOUT_MS) { currentFrom(provider) } }
            ?: lastKnown()
        return location?.let { Fix(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null) }
    }

    private suspend fun currentFrom(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        val cancel = CancellationSignal()
        continuation.invokeOnCancellation { cancel.cancel() }
        try {
            locations.getCurrentLocation(provider, cancel, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        } catch (_: SecurityException) {
            continuation.resume(null)
        }
    }

    private fun lastKnown(): Location? = try {
        PROVIDERS
            .mapNotNull { locations.getLastKnownLocation(it) }
            .filter { clock() - it.time < MAX_AGE_MS }
            .maxByOrNull { it.time }
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        const val TIMEOUT_MS = 10_000L
        const val MAX_AGE_MS = 2 * 60_000L
    }
}
