package com.offgridrescue.app.data.location

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    PENDING,
    SYNCED
}

@Entity(tableName = "location_records")
data class LocationRecordEntity(
    @PrimaryKey val id: String, // UUID
    val cloudSessionId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val syncStatus: SyncStatus
)
