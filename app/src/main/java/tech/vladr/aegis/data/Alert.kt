package tech.vladr.aegis.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A warning about one device, until you dismiss it. */
@Entity(
    tableName = "alert",
    foreignKeys = [ForeignKey(entity = Device::class, parentColumns = ["id"], childColumns = ["deviceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deviceId")],
)
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Long,
    val kind: AlertKind,
    /** For FOLLOWING alerts (v2): in how many different places the device was seen. */
    val places: Int = 0,
    val createdAt: Long,
    val dismissed: Boolean = false,
)
