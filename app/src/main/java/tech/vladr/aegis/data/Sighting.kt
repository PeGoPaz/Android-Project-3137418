package tech.vladr.aegis.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A device seen in one scan: where you were, when, and how strong its
 * signal was. The location is missing when the phone had no fix, indoors
 * for example.
 */
@Entity(
    tableName = "sighting",
    foreignKeys = [ForeignKey(entity = Device::class, parentColumns = ["id"], childColumns = ["deviceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deviceId"), Index("timestamp")],
)
data class Sighting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Long,
    val latitude: Double?,
    val longitude: Double?,
    /** How far off the location may be, in metres. */
    val accuracy: Float?,
    /** Signal strength in dBm: about -30 right next to it, -90 at the edge of reach. */
    val rssi: Int,
    val timestamp: Long,
)
