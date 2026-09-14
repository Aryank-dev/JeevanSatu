package com.offgridrescue.app.data

import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.PacketValidator
import com.offgridrescue.app.domain.SosType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class RelayStorageTest {

    @Test
    fun testValidReceivedPacketIsStored() {
        val repo = InMemoryEmergencyPacketRepository()
        val packet = createPacket(byteArrayOf(1, 2, 3, 4))
        
        assertTrue(repo.addPacket(packet))
        assertEquals(1, repo.packets.value.size)
        assertEquals(packet, repo.packets.value[0])
    }

    @Test
    fun testDuplicateDetection() {
        val repo = InMemoryEmergencyPacketRepository()
        val packetId = byteArrayOf(1, 1, 1, 1)
        val packet1 = createPacket(packetId)
        val packet2 = createPacket(packetId)

        assertTrue(repo.addPacket(packet1))
        assertFalse("Duplicate packet should not be added", repo.addPacket(packet2))
        assertEquals(1, repo.packets.value.size)
    }

    @Test
    fun testDifferentPacketsAreStoredSeparately() {
        val repo = InMemoryEmergencyPacketRepository()
        val packet1 = createPacket(byteArrayOf(1, 1, 1, 1))
        val packet2 = createPacket(byteArrayOf(2, 2, 2, 2))

        assertTrue(repo.addPacket(packet1))
        assertTrue(repo.addPacket(packet2))
        assertEquals(2, repo.packets.value.size)
    }

    @Test
    fun testOriginalFieldsArePreserved() {
        val repo = InMemoryEmergencyPacketRepository()
        val originalSourceId = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
        val originalCreatedAt = Instant.now().minusSeconds(100)
        val originalPacket = EmergencyPacket(
            packetId = byteArrayOf(9, 9, 9, 9),
            sourceDeviceId = originalSourceId,
            sosType = SosType.FIRE,
            batteryLevel = 42,
            createdAt = originalCreatedAt,
            hopCount = 2,
            ttlMinutes = 30
        )

        repo.addPacket(originalPacket)
        val stored = repo.packets.value[0]

        assertArrayEquals(originalPacket.packetId, stored.packetId)
        assertArrayEquals(originalPacket.sourceDeviceId, stored.sourceDeviceId)
        assertEquals(originalPacket.createdAt, stored.createdAt)
        assertEquals(originalPacket.sosType, stored.sosType)
        assertEquals(originalPacket.batteryLevel, stored.batteryLevel)
        assertEquals(originalPacket.hopCount, stored.hopCount)
        assertEquals(originalPacket.ttlMinutes, stored.ttlMinutes)
    }

    @Test
    fun testExpiredPacketsAreRejectedByValidator() {
        val expiredPacket = EmergencyPacket(
            packetId = byteArrayOf(1, 2, 3, 4),
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = Instant.now().minusSeconds(4000), // Expired (TTL 60m)
            ttlMinutes = 60
        )
        
        assertFalse("Expired packet should be invalid", PacketValidator.isValid(expiredPacket))
    }

    @Test
    fun testInvalidPacketsAreRejectedByValidator() {
        val invalidPacket = EmergencyPacket(
            packetId = byteArrayOf(1), // Wrong size
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 150, // Invalid battery
            createdAt = Instant.now()
        )
        assertFalse("Invalid packet should be rejected", PacketValidator.isValid(invalidPacket))
    }

    private fun createPacket(packetId: ByteArray): EmergencyPacket {
        return EmergencyPacket(
            packetId = packetId,
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            createdAt = Instant.now(),
            hopCount = 0,
            ttlMinutes = 60
        )
    }
}
