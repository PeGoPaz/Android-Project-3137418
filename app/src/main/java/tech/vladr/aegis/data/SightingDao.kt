package tech.vladr.aegis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SightingDao {
    @Insert
    suspend fun insert(sighting: Sighting): Long

    /** One device's history, newest first. */
    @Query("SELECT * FROM sighting WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun observeForDevice(deviceId: Long): Flow<List<Sighting>>

    /** Every sighting with a location, oldest first: the route and the markers on the map. */
    @Query("SELECT * FROM sighting WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp")
    fun observeLocated(): Flow<List<Sighting>>

    /** Forgets a device's history but keeps the device. */
    @Query("DELETE FROM sighting WHERE deviceId = :deviceId")
    suspend fun deleteForDevice(deviceId: Long)
}
