package tech.vladr.aegis.scan

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** What a scan gave, and whether Android let it run or handed back its last results. */
data class ScanOutcome(val networks: List<WifiNetwork>, val fresh: Boolean)

/**
 * Scans for Wi-Fi networks through Android's WifiManager.
 *
 * A scan is asked for and its results arrive later, in a broadcast. Android
 * also limits apps to four scans in two minutes: past that, startScan()
 * refuses, and the results of the last scan - by any app - are all there is.
 * Needs the precise location permission and location turned on.
 */
class WifiScanner(private val context: Context) {
    private val wifi = context.getSystemService(WifiManager::class.java)

    /** Throws SecurityException without the location permission: the screen asks for it first. */
    suspend fun scan(): ScanOutcome {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Aegis does not have the location permission")
        }
        val finished = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(received: Context, intent: Intent) {
                        context.unregisterReceiver(this)
                        if (continuation.isActive) {
                            continuation.resume(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false))
                        }
                    }
                }
                // Not exported: only the system may send this app the broadcast.
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                continuation.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
                @Suppress("DEPRECATION") // Deprecated for apps, still the only way to ask for a scan.
                if (!wifi.startScan() && continuation.isActive) {
                    context.unregisterReceiver(receiver)
                    continuation.resume(false)
                }
            }
        } ?: false
        return ScanOutcome(networks = wifi.scanResults.map(::toNetwork).sortedByDescending { it.rssi }, fresh = finished)
    }

    private fun toNetwork(result: ScanResult) = WifiNetwork(
        ssid = ssidOf(result),
        bssid = result.BSSID.lowercase(),
        security = securityOf(result.capabilities.orEmpty()),
        rssi = result.level,
        frequencyMhz = result.frequency,
    )

    private fun ssidOf(result: ScanResult): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // "MyNetwork" with its quotes, or hex digits when the name is not text.
            result.wifiSsid?.toString()?.removeSurrounding("\"").orEmpty()
        } else {
            @Suppress("DEPRECATION")
            result.SSID.orEmpty().removeSurrounding("\"")
        }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
