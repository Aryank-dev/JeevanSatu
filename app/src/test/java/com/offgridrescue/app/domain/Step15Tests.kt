package com.offgridrescue.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class Step15Tests {

    @Test
    fun testIncidentPrioritization() {
        val base = UnifiedIncident(
            sourceDeviceId = "test",
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            checkInStatus = null,
            lastSeenLocal = Instant.now(),
            lastSeenCloud = Instant.now(),
            lastKnownLat = null,
            lastKnownLng = null,
            gpsAccuracy = null,
            gpsTimestamp = null,
            localProximity = null,
            localTrend = null,
            hopCount = null,
            isDirect = null
        )

        val urgent = base.copy(checkInStatus = CheckInStatus.URGENT)
        val help = base.copy(checkInStatus = CheckInStatus.NEEDS_HELP)
        val safe = base.copy(checkInStatus = CheckInStatus.SAFE)

        assertEquals(IncidentPriority.URGENT, urgent.priority)
        assertEquals(IncidentPriority.NEEDS_HELP, help.priority)
        assertEquals(IncidentPriority.SAFE, safe.priority)
    }

    @Test
    fun testStaleCondition() {
        val staleTime = Instant.now().minusSeconds(150) // > 120s
        val staleIncident = UnifiedIncident(
            sourceDeviceId = "stale",
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            checkInStatus = CheckInStatus.URGENT,
            lastSeenLocal = staleTime,
            lastSeenCloud = staleTime,
            lastKnownLat = null,
            lastKnownLng = null,
            gpsAccuracy = null,
            gpsTimestamp = null,
            localProximity = null,
            localTrend = null,
            hopCount = null,
            isDirect = null
        )

        assertEquals("Urgent check-in should be overridden by stale condition", 
            IncidentPriority.STALE, staleIncident.priority)
    }

    @Test
    fun testPriorityOrdering() {
        val list = listOf(
            IncidentPriority.STALE,
            IncidentPriority.URGENT,
            IncidentPriority.SAFE,
            IncidentPriority.NEEDS_HELP
        )
        
        val sorted = list.sortedBy { it.ordinal }
        assertEquals(IncidentPriority.URGENT, sorted[0])
        assertEquals(IncidentPriority.NEEDS_HELP, sorted[1])
        assertEquals(IncidentPriority.SAFE, sorted[2])
        assertEquals(IncidentPriority.STALE, sorted[3])
    }
}
