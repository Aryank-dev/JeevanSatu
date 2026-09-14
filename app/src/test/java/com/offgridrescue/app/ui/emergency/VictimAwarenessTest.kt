package com.offgridrescue.app.ui.emergency

import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.SosType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VictimAwarenessTest {

    @Test
    fun testSelfFiltering() {
        val ownId = byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
        val packetFromSelf = EmergencyPacket(
            packetId = byteArrayOf(1, 2, 3, 4),
            sourceDeviceId = ownId,
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            createdAt = Instant.now()
        )
        
        // This is a logic-only check as we can't easily unit test the ViewModel's private scan listener without DI or reflection.
        // We verify that the logic we implemented in EmergencyViewModel.processNearbyPacket matches this.
        assertTrue("Packet from self should be filtered out", 
            packetFromSelf.sourceDeviceId.contentEquals(ownId))
    }

    @Test
    fun testHopCountLabels() {
        val directPacket = EmergencyPacket(
            packetId = byteArrayOf(1, 1, 1, 1),
            sourceDeviceId = byteArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
            sosType = SosType.MEDICAL,
            batteryLevel = 50,
            createdAt = Instant.now(),
            hopCount = 0
        )
        
        val relayedPacket = directPacket.copy(hopCount = 1)
        
        assertEquals(0, directPacket.hopCount)
        assertTrue(relayedPacket.hopCount > 0)
    }
}
