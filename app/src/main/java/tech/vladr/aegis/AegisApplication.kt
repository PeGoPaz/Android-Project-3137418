package tech.vladr.aegis

import android.app.Application
import tech.vladr.aegis.data.AegisDatabase
import tech.vladr.aegis.data.AegisRepository
import tech.vladr.aegis.scan.WifiScanner
import tech.vladr.aegis.sensors.Locator

/**
 * Created by Android once, before any screen. It holds what the whole app
 * shares - the database first - so that every screen uses the same one.
 */
class AegisApplication : Application() {
    val database: AegisDatabase by lazy { AegisDatabase.create(this) }
    val repository: AegisRepository by lazy { AegisRepository(database) }
    val scanner: WifiScanner by lazy { WifiScanner(this) }
    val locator: Locator by lazy { Locator(this) }
}
