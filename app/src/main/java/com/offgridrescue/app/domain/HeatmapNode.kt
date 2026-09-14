package com.offgridrescue.app.domain

import java.time.Instant

data class HeatmapNode(
    val deviceId: String,
    val proximity: ProximityLevel,
    val signalTrend: SignalTrend,
    val isDirect: Boolean,
    val sosType: SosType?,
    val batteryLevel: Int?,
    val lastSeen: Instant
)
