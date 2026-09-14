package com.offgridrescue.app.domain

import java.time.Instant

object PacketValidator {
    const val MAX_HOPS = 5
    const val SUPPORTED_VERSION = 1
    const val PACKET_ID_SIZE = 4
    const val DEVICE_ID_SIZE = 8

    fun isValid(packet: EmergencyPacket): Boolean {
        if (packet.protocolVersion != SUPPORTED_VERSION) return false
        if (packet.packetId.size != PACKET_ID_SIZE) return false
        if (packet.sourceDeviceId.size != DEVICE_ID_SIZE) return false
        if (packet.batteryLevel !in 0..100) return false
        if (packet.hopCount !in 0..MAX_HOPS) return false
        
        val now = Instant.now()
        if (packet.createdAt.isAfter(now.plusSeconds(30))) return false // Allow slight clock drift
        
        return !isExpired(packet)
    }

    fun isExpired(packet: EmergencyPacket): Boolean {
        val now = Instant.now()
        val expiration = packet.createdAt.plusSeconds(packet.ttlMinutes.toLong() * 60)
        return now.isAfter(expiration)
    }
}
