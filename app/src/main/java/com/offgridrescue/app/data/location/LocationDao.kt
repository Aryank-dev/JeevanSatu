package com.offgridrescue.app.data.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: LocationRecordEntity)

    @Query("SELECT * FROM location_records WHERE syncStatus = 'PENDING' AND cloudSessionId = :sessionId")
    suspend fun getPendingRecords(sessionId: String): List<LocationRecordEntity>

    @Query("UPDATE location_records SET syncStatus = 'SYNCED' WHERE id = :recordId")
    suspend fun markAsSynced(recordId: String)

    @Query("SELECT * FROM location_records WHERE cloudSessionId = :sessionId ORDER BY timestamp DESC")
    fun getRecordsForSession(sessionId: String): Flow<List<LocationRecordEntity>>
}
