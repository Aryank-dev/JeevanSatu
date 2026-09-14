package com.offgridrescue.app.data

import com.offgridrescue.app.ble.PacketSerializer
import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.PacketValidator
import com.offgridrescue.app.domain.SosType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class RelayForwardingTest {

    @Test
    fun testHopCountIncrement() {
        val original = createPacket(hopCount = 0)
        val forwarded = original.copy(hopCount = original.hopCount + 1)
        
        assertEquals(1, forwarded.hopCount)
        assertEquals(original.packetId, forwarded.packetId)
    }

    @Test
    fun testForwardedPacketIdentityPreservation() {
        val original = EmergencyPacket(
            packetId = byteArrayOf(1, 2, 3, 4),
            sourceDeviceId = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            sosType = SosType.MEDICAL,
            batteryLevel = 88,
            createdAt = Instant.now(),
            hopCount = 1,
            ttlMinutes = 45
        )
        
        val forwarded = original.copy(hopCount = original.hopCount + 1)
        
        assertArrayEquals(original.packetId, forwarded.packetId)
        assertArrayEquals(original.sourceDeviceId, forwarded.sourceDeviceId)
        assertEquals(original.sosType, forwarded.sosType)
        assertEquals(original.batteryLevel, forwarded.batteryLevel)
        assertEquals(original.createdAt.epochSecond, forwarded.createdAt.epochSecond)
        assertEquals(original.ttlMinutes, forwarded.ttlMinutes)
        assertEquals(2, forwarded.hopCount)
    }

    @Test
    fun testMaxHopsEnforcement() {
        val maxHopsPacket = createPacket(hopCount = 5)
        assertFalse("Packet with hopCount 5 should be invalid for further forwarding", 
            maxHopsPacket.hopCount < PacketValidator.MAX_HOPS)
            
        val almostMax = createPacket(hopCount = 4)
        assertTrue("Packet with hopCount 4 should be eligible for forwarding once more", 
            almostMax.hopCount < PacketValidator.MAX_HOPS)
    }

    @Test
    fun testForwardingDeduplication() {
        val repo = InMemoryEmergencyPacketRepository()
        val packetId = byteArrayOf(1, 2, 3, 4)
        
        assertFalse(repo.isForwarded(packetId))
        repo.markAsForwarded(packetId)
        assertTrue(repo.isForwarded(packetId))
    }

    @Test
    fun testExpirationValidation() {
        val expired = EmergencyPacket(
            packetId = ByteArray(4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = Instant.now().minusSeconds(3601),
            ttlMinutes = 60
        )
        assertTrue(PacketValidator.isExpired(expired))
        
        val valid = EmergencyPacket(
            packetId = ByteArray(4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = Instant.now(),
            ttlMinutes = 60
        )
        assertFalse(PacketValidator.isExpired(valid))
    }

    @Test
    fun testSerializationRoundTripForForwardedPacket() {
        val original = createPacket(hopCount = 2)
        val serialized = PacketSerializer.serialize(original)
        val deserialized = PacketSerializer.deserialize(serialized)
        
        assertNotNull(deserialized)
        assertEquals(2, deserialized?.hopCount)
        // Compare epochSeconds because nanoseconds are not preserved in binary format
        assertEquals(original.createdAt.epochSecond, deserialized?.createdAt?.epochSecond)
        assertEquals(original.packetId.toList(), deserialized?.packetId?.toList())
        assertEquals(original.sosType, deserialized?.sosType)
    }

    @Test
    fun testPayloadSizeRemainsConstant() {
        val hop0 = createPacket(hopCount = 0)
        val hop5 = createPacket(hopCount = 5)
        
        assertEquals(21, PacketSerializer.serialize(hop0).size)
        assertEquals(21, PacketSerializer.serialize(hop5).size)
    }

    private fun createPacket(hopCount: Int): EmergencyPacket {
        return EmergencyPacket(
            packetId = byteArrayOf(1, 2, 3, 4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            createdAt = Instant.now(),
            hopCount = hopCount,
            ttlMinutes = 60
        )
    }
}
