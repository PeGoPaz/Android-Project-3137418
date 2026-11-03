package tech.vladr.aegis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's SQLite database. Room writes the code behind the DAOs while the
 * app is built, and checks every query against these tables.
 */
@Database(entities = [Device::class, Sighting::class, Alert::class], version = 1)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun devices(): DeviceDao
    abstract fun sightings(): SightingDao
    abstract fun alerts(): AlertDao

    companion object {
        fun create(context: Context): AegisDatabase =
            Room.databaseBuilder(context, AegisDatabase::class.java, "aegis.db").build()
    }
}
