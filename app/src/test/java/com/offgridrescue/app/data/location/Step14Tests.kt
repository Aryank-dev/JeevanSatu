package com.offgridrescue.app.data.location

import com.offgridrescue.app.domain.AlertSeverity
import com.offgridrescue.app.domain.CheckInStatus
import com.offgridrescue.app.domain.OfficialAlert
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class Step14Tests {

    @Test
    fun testCheckInStatusPreservation() {
        val status = CheckInStatus.URGENT
        assertEquals("Urgent", status.displayName)
    }

    @Test
    fun testSimulatedAlertLabeling() {
        val alert = OfficialAlert(
            id = "test",
            title = "Flood",
            message = "...",
            severity = AlertSeverity.WARNING,
            timestamp = Instant.now()
        )
        assertTrue("Alert title must contain [SIMULATED]", alert.displayTitle.contains("[SIMULATED]"))
    }

    @Test
    fun testCheckInRecordCreation() {
        val record = CheckInRecordEntity(
            id = "uuid",
            cloudSessionId = "sid",
            status = CheckInStatus.SAFE,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        assertEquals(CheckInStatus.SAFE, record.status)
        assertEquals(SyncStatus.PENDING, record.syncStatus)
    }

    @Test
    fun testCheckInSyncTransition() {
        val record = CheckInRecordEntity(
            id = "uuid",
            cloudSessionId = "sid",
            status = CheckInStatus.SAFE,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        val synced = record.copy(syncStatus = SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, synced.syncStatus)
    }
}
