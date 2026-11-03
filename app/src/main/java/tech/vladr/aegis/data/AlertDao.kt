package tech.vladr.aegis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: Alert): Long

    /** Alerts not dismissed yet, newest first, each with its device. */
    @Query(
        """
        SELECT alert.*, device.name AS deviceName, device.customName AS deviceCustomName,
               device.address AS deviceAddress, device.security AS deviceSecurity
        FROM alert JOIN device ON device.id = alert.deviceId
        WHERE alert.dismissed = 0
        ORDER BY alert.createdAt DESC
        """,
    )
    fun observeActive(): Flow<List<AlertWithDevice>>

    @Query("SELECT COUNT(*) FROM alert WHERE deviceId = :deviceId AND kind = :kind AND dismissed = 0")
    suspend fun countActive(deviceId: Long, kind: AlertKind): Int

    @Query("UPDATE alert SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)
}
