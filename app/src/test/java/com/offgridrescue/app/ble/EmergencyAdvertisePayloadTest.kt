package com.offgridrescue.app.ble

import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.SosType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EmergencyAdvertisePayloadTest {
    @Test
    fun build_containsMarkerAndSerializedPacket() {
        val packet = EmergencyPacket(
            packetId = byteArrayOf(1, 2, 3, 4),
            sourceDeviceId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
            sosType = SosType.GENERAL,
            batteryLevel = 100,
            createdAt = Instant.now()
        )
        
        val payload = EmergencyAdvertisePayload.build(packet)
        
        // Marker (2) + Packet (21) = 23 bytes
        assertEquals(23, payload.size)
        assertEquals(0x0F.toByte(), payload[0])
        assertEquals(0x52.toByte(), payload[1])
    }
}
