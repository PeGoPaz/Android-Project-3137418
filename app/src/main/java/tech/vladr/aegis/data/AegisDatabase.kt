package tech.vladr.aegis.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's SQLite database. Room writes the code behind the DAOs while the
 * app is built, and checks every query against these tables.
 *
 * Each version's tables are kept in app/schemas. Version 2 adds the tracker
 * kind of a Bluetooth device; Room writes that migration itself, since a new
 * column that may be empty needs nothing from the rows already there.
 */
@Database(
    entities = [Device::class, Sighting::class, Alert::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun devices(): DeviceDao
    abstract fun sightings(): SightingDao
    abstract fun alerts(): AlertDao

    companion object {
        fun create(context: Context): AegisDatabase =
            Room.databaseBuilder(context, AegisDatabase::class.java, "aegis.db").build()
    }
}
