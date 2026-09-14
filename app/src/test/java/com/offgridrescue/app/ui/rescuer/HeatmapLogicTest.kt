package com.offgridrescue.app.ui.rescuer

import com.offgridrescue.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HeatmapLogicTest {

    @Test
    fun testHeatmapNodeMapping() {
        val deviceProximity = DeviceProximity(
            deviceId = "test-device",
            rawRssi = -50,
            smoothedRssi = -50.0,
            proximity = ProximityLevel.VERY_CLOSE,
            signalTrend = SignalTrend.STRONGER,
            lastSeen = Instant.now(),
            rssiHistory = emptyList(),
            latestPacket = EmergencyPacket(
                packetId = byteArrayOf(1, 2, 3, 4),
                sourceDeviceId = ByteArray(8),
                sosType = SosType.MEDICAL,
                batteryLevel = 90,
                createdAt = Instant.now(),
                hopCount = 0
            )
        )

        // Mapping logic simulation (as used in RescuerViewModel)
        val node = HeatmapNode(
            deviceId = deviceProximity.deviceId,
            proximity = deviceProximity.proximity,
            signalTrend = deviceProximity.signalTrend,
            isDirect = (deviceProximity.latestPacket?.hopCount ?: 0) == 0,
            sosType = deviceProximity.latestPacket?.sosType,
            batteryLevel = deviceProximity.latestPacket?.batteryLevel,
            lastSeen = deviceProximity.lastSeen
        )

        assertEquals("test-device", node.deviceId)
        assertTrue(node.isDirect)
        assertEquals(SosType.MEDICAL, node.sosType)
        assertEquals(ProximityLevel.VERY_CLOSE, node.proximity)
    }

    @Test
    fun testRelayedPacketIsMarkedNotDirect() {
        val relayedDevice = DeviceProximity(
            deviceId = "relayed-device",
            rawRssi = -80,
            smoothedRssi = -80.0,
            proximity = ProximityLevel.FAR,
            signalTrend = SignalTrend.STABLE,
            lastSeen = Instant.now(),
            rssiHistory = emptyList(),
            latestPacket = EmergencyPacket(
                packetId = byteArrayOf(1, 2, 3, 4),
                sourceDeviceId = ByteArray(8),
                sosType = SosType.GENERAL,
                batteryLevel = 50,
                createdAt = Instant.now(),
                hopCount = 1 // RELAYED
            )
        )

        val node = HeatmapNode(
            deviceId = relayedDevice.deviceId,
            proximity = relayedDevice.proximity,
            signalTrend = relayedDevice.signalTrend,
            isDirect = (relayedDevice.latestPacket?.hopCount ?: 0) == 0,
            sosType = relayedDevice.latestPacket?.sosType,
            batteryLevel = relayedDevice.latestPacket?.batteryLevel,
            lastSeen = relayedDevice.lastSeen
        )

        assertFalse("Relayed packet should not be marked as direct", node.isDirect)
    }
}
