package tech.vladr.aegis.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import tech.vladr.aegis.MainActivity
import tech.vladr.aegis.R
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.FollowingListener
import tech.vladr.aegis.data.SettingsSource
import tech.vladr.aegis.ui.detail.DeviceDetailActivity

/**
 * Puts a "following you" alert in the notification shade, so it is seen even
 * with Aegis in the background, as it will be on Trip mode's scans (v3).
 * Tapping it opens the device's page, with the app's main screen behind it.
 *
 * Nothing is shown when the user turned these off in Settings, or did not
 * allow notifications (asked for on Android 13 and later).
 */
class FollowingNotifier(private val context: Context, private val settings: SettingsSource) : FollowingListener {

    override suspend fun startedFollowing(device: Device, places: Int) {
        if (!settings.settings.first().followingNotifications) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(channel())
        // One notification per device: if it is still there, the new one replaces it.
        manager.notify(TAG, device.id.toInt(), notification(device, places))
    }

    private fun channel() = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
        .setName("Devices following you")
        .setDescription("A tracker or other device seen in several places you went")
        .build()

    private fun notification(device: Device, places: Int) = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_shield)
        .setContentTitle("${device.displayName} may be following you")
        .setContentText("Seen in $places places you went. Tap to see where.")
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "Seen in $places places you went in the last few hours. Tap to see where and when, " +
                    "and mark it as yours if it is.",
            ),
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(openDevice(device))
        .setAutoCancel(true)
        .build()

    /**
     * What a tap does, handed to Android to run later on Aegis's behalf.
     * Immutable, so no other app holding it can change where it leads;
     * Android 12 and later refuse a PendingIntent that does not say which it is.
     */
    private fun openDevice(device: Device): PendingIntent? = TaskStackBuilder.create(context)
        .addNextIntent(Intent(context, MainActivity::class.java))
        .addNextIntent(DeviceDetailActivity.intent(context, device.id))
        .getPendingIntent(device.id.toInt(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    companion object {
        const val CHANNEL = "following"
        const val TAG = "following"
    }
}
