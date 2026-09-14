package com.offgridrescue.app.data

import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.SosType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class EmergencyPacketRepositoryTest {

    @Test
    fun testDuplicateDetection() {
        val repo = InMemoryEmergencyPacketRepository()
        val packetId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        
        val packet1 = EmergencyPacket(
            packetId = packetId,
            sourceDeviceId = ByteArray(8),
            sosType = SosType.GENERAL,
            batteryLevel = 50,
            createdAt = Instant.now()
        )
        
        val packet2 = EmergencyPacket(
            packetId = packetId, // SAME packetId
            sourceDeviceId = ByteArray(8),
            sosType = SosType.MEDICAL,
            batteryLevel = 60,
            createdAt = Instant.now()
        )

        assertTrue(repo.addPacket(packet1))
        assertFalse("Should reject duplicate packetId", repo.addPacket(packet2))
        assertEquals(1, repo.packets.value.size)
    }
}
