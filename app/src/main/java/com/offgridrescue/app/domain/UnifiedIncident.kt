package com.offgridrescue.app.domain

import java.time.Instant

enum class IncidentPriority {
    URGENT,
    NEEDS_HELP,
    SAFE,
    STALE
}

data class UnifiedIncident(
    val sourceDeviceId: String,
    val sosType: SosType?,
    val batteryLevel: Int?,
    val checkInStatus: CheckInStatus?,
    val lastSeenLocal: Instant?,
    val lastSeenCloud: Instant?,
    val lastKnownLat: Double?,
    val lastKnownLng: Double?,
    val gpsAccuracy: Float?,
    val gpsTimestamp: Instant?,
    val localProximity: ProximityLevel?,
    val localTrend: SignalTrend?,
    val hopCount: Int?,
    val isDirect: Boolean?
) {
    val priority: IncidentPriority
        get() {
            // Derived priority based on check-in status
            val basePriority = when (checkInStatus) {
                CheckInStatus.URGENT -> IncidentPriority.URGENT
                CheckInStatus.NEEDS_HELP -> IncidentPriority.NEEDS_HELP
                CheckInStatus.SAFE -> IncidentPriority.SAFE
                null -> IncidentPriority.NEEDS_HELP // Default if no check-in but active emergency
            }

            // Flag as stale if no update from either source in 2 minutes
            val now = Instant.now()
            val localStale = lastSeenLocal?.let { java.time.Duration.between(it, now).seconds > 120 } ?: true
            val cloudStale = lastSeenCloud?.let { java.time.Duration.between(it, now).seconds > 120 } ?: true

            return if (localStale && cloudStale) IncidentPriority.STALE else basePriority
        }
}
