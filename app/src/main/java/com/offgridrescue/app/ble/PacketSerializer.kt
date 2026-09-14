package com.offgridrescue.app.ble

import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.SosType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

object PacketSerializer {
    private const val PACKET_SIZE = 21

    fun serialize(packet: EmergencyPacket): ByteArray {
        val buffer = ByteBuffer.allocate(PACKET_SIZE)
        buffer.order(ByteOrder.BIG_ENDIAN)

        buffer.put(packet.protocolVersion.toByte())
        buffer.put(packet.packetId)
        buffer.put(packet.sourceDeviceId)
        buffer.put(packet.sosType.id.toByte())
        buffer.put(packet.batteryLevel.toByte())
        
        // Unsigned 32-bit Unix timestamp
        val unixTime = packet.createdAt.epochSecond
        buffer.putInt((unixTime and 0xFFFFFFFFL).toInt())
        
        buffer.put(packet.hopCount.toByte())
        buffer.put(packet.ttlMinutes.toByte())

        return buffer.array()
    }

    fun deserialize(bytes: ByteArray): EmergencyPacket? {
        if (bytes.size != PACKET_SIZE) return null
        
        return try {
            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.BIG_ENDIAN)

            val version = buffer.get().toInt() and 0xFF
            val packetId = ByteArray(4)
            buffer.get(packetId)
            val sourceId = ByteArray(8)
            buffer.get(sourceId)
            val sosTypeId = buffer.get().toInt() and 0xFF
            val battery = buffer.get().toInt() and 0xFF
            
            // Unsigned 32-bit Unix timestamp
            val unixTime = buffer.getInt().toLong() and 0xFFFFFFFFL
            val createdAt = Instant.ofEpochSecond(unixTime)
            
            val hopCount = buffer.get().toInt() and 0xFF
            val ttl = buffer.get().toInt() and 0xFF

            EmergencyPacket(
                protocolVersion = version,
                packetId = packetId,
                sourceDeviceId = sourceId,
                sosType = SosType.fromId(sosTypeId),
                batteryLevel = battery,
                createdAt = createdAt,
                hopCount = hopCount,
                ttlMinutes = ttl
            )
        } catch (e: Exception) {
            null
        }
    }
}
