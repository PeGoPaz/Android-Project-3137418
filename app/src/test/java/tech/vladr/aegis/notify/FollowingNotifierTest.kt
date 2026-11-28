package tech.vladr.aegis.notify

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.vladr.aegis.MainActivity
import tech.vladr.aegis.ble.TrackerKind
import tech.vladr.aegis.data.Device
import tech.vladr.aegis.data.DeviceKind
import tech.vladr.aegis.data.Settings
import tech.vladr.aegis.data.SettingsSource
import tech.vladr.aegis.ui.detail.DeviceDetailActivity

/** The notification for a device following you, against Robolectric's notification manager. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FollowingNotifierTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val shade = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val airtag = Device(id = 7, address = "de:ad:be:ef:00:01", name = "", kind = DeviceKind.BLE, tracker = TrackerKind.AIRTAG, firstSeen = 0, lastSeen = 0)

    private fun notifier(on: Boolean = true) = FollowingNotifier(app, object : SettingsSource {
        override val settings = flowOf(Settings(followingNotifications = on))
    })

    private fun allowed() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun aDeviceFollowingYouIsNotified() = runTest {
        allowed()
        notifier().startedFollowing(airtag, places = 3)

        val notification = shade.allNotifications.single()
        assertEquals("AirTag may be following you", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Seen in 3 places you went. Tap to see where.", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, app.getSystemService(NotificationManager::class.java).getNotificationChannel(FollowingNotifier.CHANNEL).importance)
    }

    @Test
    fun tappingItOpensTheDevicePageOverTheApp() = runTest {
        allowed()
        notifier().startedFollowing(airtag, places = 3)

        val tap = shadowOf(shade.allNotifications.single().contentIntent)
        assertTrue("must be immutable", tap.isImmutable)
        val screens = tap.savedIntents.map { it.component!!.className }
        assertEquals(listOf(MainActivity::class.java.name, DeviceDetailActivity::class.java.name), screens)
    }

    @Test
    fun theSameDeviceAgainReplacesItsNotification() = runTest {
        allowed()
        notifier().startedFollowing(airtag, places = 3)
        notifier().startedFollowing(airtag, places = 4)

        val notification = shade.allNotifications.single()
        assertEquals("Seen in 4 places you went. Tap to see where.", notification.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test
    fun turnedOffInSettingsNothingIsShown() = runTest {
        allowed()
        notifier(on = false).startedFollowing(airtag, places = 3)
        assertTrue(shade.allNotifications.isEmpty())
    }

    @Test
    fun withoutThePermissionNothingIsShown() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier().startedFollowing(airtag, places = 3)
        assertTrue(shade.allNotifications.isEmpty())
    }
}
