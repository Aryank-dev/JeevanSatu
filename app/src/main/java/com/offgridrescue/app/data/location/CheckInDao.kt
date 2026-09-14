package com.offgridrescue.app.data.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecordEntity)

    @Query("SELECT * FROM check_in_records WHERE syncStatus = 'PENDING' AND cloudSessionId = :sessionId")
    suspend fun getPendingRecords(sessionId: String): List<CheckInRecordEntity>

    @Query("UPDATE check_in_records SET syncStatus = 'SYNCED' WHERE id = :recordId")
    suspend fun markAsSynced(recordId: String)

    @Query("SELECT * FROM check_in_records WHERE cloudSessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestCheckIn(sessionId: String): Flow<CheckInRecordEntity?>
}
