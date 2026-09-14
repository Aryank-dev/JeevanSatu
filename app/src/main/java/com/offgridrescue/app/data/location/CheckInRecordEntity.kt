package com.offgridrescue.app.data.location

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.offgridrescue.app.domain.CheckInStatus

@Entity(tableName = "check_in_records")
data class CheckInRecordEntity(
    @PrimaryKey val id: String, // UUID
    val cloudSessionId: String,
    val status: CheckInStatus,
    val timestamp: Long,
    val syncStatus: SyncStatus
)
