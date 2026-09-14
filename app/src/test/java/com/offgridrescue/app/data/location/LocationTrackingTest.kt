package com.offgridrescue.app.data.location

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LocationTrackingTest {

    @Test
    fun testLocationRecordCreation() {
        val sessionId = "test-session"
        val recordId = UUID.randomUUID().toString()
        val record = LocationRecordEntity(
            id = recordId,
            cloudSessionId = sessionId,
            latitude = 1.23,
            longitude = 4.56,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )

        assertEquals(recordId, record.id)
        assertEquals(sessionId, record.cloudSessionId)
        assertEquals(SyncStatus.PENDING, record.syncStatus)
        assertEquals(1.23, record.latitude, 0.001)
    }

    @Test
    fun testSyncStatusTransition() {
        val record = LocationRecordEntity(
            id = "id",
            cloudSessionId = "sid",
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 0f,
            timestamp = 0L,
            syncStatus = SyncStatus.PENDING
        )
        
        val syncedRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, syncedRecord.syncStatus)
    }
}
