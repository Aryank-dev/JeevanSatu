package com.offgridrescue.app.domain

import com.offgridrescue.app.ble.AnonymousDeviceId
import java.time.Instant

data class EmergencyPacket(
    val protocolVersion: Int = 1,
    val packetId: ByteArray,
    val sourceDeviceId: ByteArray,
    val sosType: SosType,
    val batteryLevel: Int,
    val createdAt: Instant,
    val hopCount: Int = 0,
    val ttlMinutes: Int = 60 // Default 1 hour
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmergencyPacket

        if (protocolVersion != other.protocolVersion) return false
        if (!packetId.contentEquals(other.packetId)) return false
        if (!sourceDeviceId.contentEquals(other.sourceDeviceId)) return false
        if (sosType != other.sosType) return false
        if (batteryLevel != other.batteryLevel) return false
        if (createdAt != other.createdAt) return false
        if (hopCount != other.hopCount) return false
        if (ttlMinutes != other.ttlMinutes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = protocolVersion
        result = 31 * result + packetId.contentHashCode()
        result = 31 * result + sourceDeviceId.contentHashCode()
        result = 31 * result + sosType.hashCode()
        result = 31 * result + batteryLevel
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + hopCount
        result = 31 * result + ttlMinutes
        return result
    }
    
    fun toHexPacketId(): String = packetId.joinToString("") { "%02x".format(it) }
    fun toHexSourceId(): String = AnonymousDeviceId.toHex(sourceDeviceId)
}
