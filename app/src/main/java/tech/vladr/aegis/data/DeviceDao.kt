package tech.vladr.aegis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Reading and changing devices. Queries that return a Flow send a new list whenever the table changes. */
@Dao
interface DeviceDao {
    @Insert
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Query("SELECT * FROM device WHERE address = :address")
    suspend fun findByAddress(address: String): Device?

    @Query("SELECT * FROM device WHERE id = :id")
    fun observe(id: Long): Flow<Device?>

    @Query("SELECT * FROM device ORDER BY lastSeen DESC")
    fun observeAll(): Flow<List<Device>>

    /** Trusted ones first, then by name. */
    @Query("SELECT * FROM device WHERE status IN (:statuses) ORDER BY status != 'TRUSTED', COALESCE(customName, name)")
    fun observeWithStatus(statuses: List<DeviceStatus>): Flow<List<Device>>

    @Query("UPDATE device SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: DeviceStatus)

    @Query("UPDATE device SET customName = :customName WHERE id = :id")
    suspend fun rename(id: Long, customName: String?)

    /** Its sightings and alerts go with it (ON DELETE CASCADE). */
    @Query("DELETE FROM device WHERE id = :id")
    suspend fun delete(id: Long)
}
