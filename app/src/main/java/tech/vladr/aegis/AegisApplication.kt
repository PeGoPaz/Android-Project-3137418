package tech.vladr.aegis

import android.app.Application
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import tech.vladr.aegis.ble.BleScanner
import tech.vladr.aegis.data.AegisDatabase
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.data.SettingsStore
import tech.vladr.aegis.notify.FollowingNotifier
import tech.vladr.aegis.scan.WifiScanner
import tech.vladr.aegis.sensors.Compass
import tech.vladr.aegis.sensors.Locator

/**
 * Created by Android once, before any screen. It holds what the whole app
 * shares - the database first - so that every screen uses the same one.
 */
class AegisApplication : Application() {
    val database: AegisDatabase by lazy { AegisDatabase.create(this) }
    val repository: AegisRepository by lazy { AegisRepository(database, FollowingNotifier(this, settings)) }
    val scanner: WifiScanner by lazy { WifiScanner(this) }
    val bluetooth: BleScanner by lazy { BleScanner(this) }
    val locator: Locator by lazy { Locator(this) }
    // The screen's rotation from the display itself, not from an activity, so the compass outlives one.
    val compass: Compass by lazy {
        Compass(this) { getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0 }
    }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}
