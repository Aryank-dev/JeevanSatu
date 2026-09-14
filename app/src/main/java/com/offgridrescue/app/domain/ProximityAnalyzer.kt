package com.offgridrescue.app.domain

import java.time.Instant
import kotlin.math.abs

class ProximityAnalyzer(
    private val windowSize: Int = 10,
    private val smoothingAlpha: Double = 0.3,
    private val stableThreshold: Double = 1.5
) {
    fun analyze(deviceId: String, newRssi: Int, existingData: DeviceProximity?): DeviceProximity {
        val history = ((existingData?.rssiHistory ?: emptyList()) + newRssi).takeLast(windowSize)
        
        val smoothedRssi = if (existingData == null) {
            newRssi.toDouble()
        } else {
            smoothingAlpha * newRssi + (1 - smoothingAlpha) * existingData.smoothedRssi
        }

        val proximity = classify(smoothedRssi)
        val trend = calculateTrend(smoothedRssi, existingData?.smoothedRssi)

        return DeviceProximity(
            deviceId = deviceId,
            rawRssi = newRssi,
            smoothedRssi = smoothedRssi,
            proximity = proximity,
            signalTrend = trend,
            lastSeen = Instant.now(),
            rssiHistory = history
        )
    }

    private fun classify(rssi: Double): ProximityLevel {
        return when {
            rssi >= -55 -> ProximityLevel.VERY_CLOSE
            rssi >= -70 -> ProximityLevel.NEARBY
            rssi < -70 -> ProximityLevel.FAR
            else -> ProximityLevel.UNKNOWN
        }
    }

    private fun calculateTrend(currentSmoothed: Double, previousSmoothed: Double?): SignalTrend {
        if (previousSmoothed == null) return SignalTrend.UNKNOWN
        
        val diff = currentSmoothed - previousSmoothed
        return when {
            abs(diff) < stableThreshold -> SignalTrend.STABLE
            diff > 0 -> SignalTrend.STRONGER
            else -> SignalTrend.WEAKER
        }
    }
}
