package com.offgridrescue.app.ble

import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.SosType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class PacketSerializerTest {

    @Test
    fun testSerializationRoundTrip() {
        val packetId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val sourceId = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11)
        val now = Instant.ofEpochSecond(1724850000) // Fixed time

        val original = EmergencyPacket(
            protocolVersion = 1,
            packetId = packetId,
            sourceDeviceId = sourceId,
            sosType = SosType.MEDICAL,
            batteryLevel = 85,
            createdAt = now,
            hopCount = 0,
            ttlMinutes = 60
        )

        val serialized = PacketSerializer.serialize(original)
        assertEquals(21, serialized.size)

        val deserialized = PacketSerializer.deserialize(serialized)
        assertNotNull(deserialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testFutureTimestampHandling() {
        // Test a timestamp that would be negative if interpreted as signed 32-bit (e.g., > 2038)
        val futureTime = Instant.ofEpochSecond(3000000000L) 
        val original = EmergencyPacket(
            packetId = byteArrayOf(0, 0, 0, 0),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = futureTime,
            ttlMinutes = 60
        )

        val serialized = PacketSerializer.serialize(original)
        val deserialized = PacketSerializer.deserialize(serialized)
        
        assertNotNull(deserialized)
        assertEquals(futureTime.epochSecond, deserialized?.createdAt?.epochSecond)
    }

    @Test
    fun testInvalidSizeRejection() {
        assertNull(PacketSerializer.deserialize(ByteArray(20)))
        assertNull(PacketSerializer.deserialize(ByteArray(22)))
    }
}
