package com.offgridrescue.app.domain

import com.offgridrescue.app.domain.EmergencyPacket
import java.time.Instant

enum class ProximityLevel(val displayName: String) {
    VERY_CLOSE("VERY CLOSE"),
    NEARBY("NEARBY"),
    FAR("FAR"),
    UNKNOWN("UNKNOWN")
}

enum class SignalTrend(val displayName: String, val symbol: String) {
    STRONGER("GETTING STRONGER", "↑"),
    WEAKER("GETTING WEAKER", "↓"),
    STABLE("STABLE", "→"),
    UNKNOWN("UNKNOWN", "")
}

data class DeviceProximity(
    val deviceId: String,
    val rawRssi: Int,
    val smoothedRssi: Double,
    val proximity: ProximityLevel,
    val signalTrend: SignalTrend,
    val lastSeen: Instant = Instant.now(),
    val rssiHistory: List<Int> = emptyList(),
    val latestPacket: EmergencyPacket? = null
)
