package com.offgridrescue.app.domain

import com.offgridrescue.app.ui.emergency.CloudSyncStatus
import com.offgridrescue.app.ui.emergency.GpsStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SyncRefinementTests {

    @Test
    fun testGpsStatusMapping() {
        assertEquals("Active", GpsStatus.ACTIVE.displayName)
        assertEquals("Unavailable", GpsStatus.UNAVAILABLE.displayName)
        assertEquals("Permission Denied", GpsStatus.PERMISSION_DENIED.displayName)
    }

    @Test
    fun testSyncStatusMapping() {
        assertEquals("Synced", CloudSyncStatus.SYNCED.displayName)
        assertEquals("Pending", CloudSyncStatus.PENDING.displayName)
        assertEquals("Sync Error", CloudSyncStatus.ERROR.displayName)
        assertEquals("Auth Error", CloudSyncStatus.AUTH_ERROR.displayName)
    }

    @Test
    fun testStaleThreshold() {
        // Logic check for the 120s stale threshold defined in UnifiedIncident
        val now = Instant.now()
        val fresh = now.minusSeconds(30)
        val stale = now.minusSeconds(130)
        
        val incident = UnifiedIncident(
            sourceDeviceId = "test",
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            checkInStatus = CheckInStatus.SAFE,
            lastSeenLocal = fresh,
            lastSeenCloud = fresh,
            lastKnownLat = null,
            lastKnownLng = null,
            gpsAccuracy = null,
            gpsTimestamp = null,
            localProximity = null,
            localTrend = null,
            hopCount = null,
            isDirect = null
        )
        
        assertEquals(IncidentPriority.SAFE, incident.priority)
        
        val staleIncident = incident.copy(lastSeenLocal = stale, lastSeenCloud = stale)
        assertEquals(IncidentPriority.STALE, staleIncident.priority)
    }
}
