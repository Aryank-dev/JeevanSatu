package com.offgridrescue.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class PacketValidatorTest {

    @Test
    fun testValidPacket() {
        val packet = EmergencyPacket(
            packetId = ByteArray(4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            createdAt = Instant.now(),
            hopCount = 0,
            ttlMinutes = 60
        )
        assertTrue(PacketValidator.isValid(packet))
    }

    @Test
    fun testInvalidBattery() {
        val packet = EmergencyPacket(
            packetId = ByteArray(4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 101, // Invalid
            createdAt = Instant.now()
        )
        assertFalse(PacketValidator.isValid(packet))
    }

    @Test
    fun testInvalidHopCount() {
        val packet = EmergencyPacket(
            packetId = ByteArray(4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = Instant.now(),
            hopCount = 6 // Max is 5
        )
        assertFalse(PacketValidator.isValid(packet))
    }

    @Test
    fun testExpiredPacket() {
        val oldPacket = EmergencyPacket(
            packetId = ByteArray(4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = Instant.now().minusSeconds(3700), // > 1 hour ago
            ttlMinutes = 60
        )
        assertFalse(PacketValidator.isValid(oldPacket))
    }
}
