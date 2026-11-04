package tech.vladr.aegis

import android.app.Application
import tech.vladr.aegis.data.AegisDatabase
import tech.vladr.aegis.scan.WifiScanner

/**
 * Created by Android once, before any screen. It holds what the whole app
 * shares - the database first - so that every screen uses the same one.
 */
class AegisApplication : Application() {
    val database: AegisDatabase by lazy { AegisDatabase.create(this) }
    val scanner: WifiScanner by lazy { WifiScanner(this) }
}
