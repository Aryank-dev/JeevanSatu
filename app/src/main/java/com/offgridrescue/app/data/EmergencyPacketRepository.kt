package com.offgridrescue.app.data

import com.offgridrescue.app.domain.EmergencyPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface EmergencyPacketRepository {
    val packets: StateFlow<List<EmergencyPacket>>
    fun addPacket(packet: EmergencyPacket): Boolean
    fun markAsForwarded(packetId: ByteArray)
    fun isForwarded(packetId: ByteArray): Boolean
    fun clear()
}

class InMemoryEmergencyPacketRepository : EmergencyPacketRepository {
    private val _packets = MutableStateFlow<List<EmergencyPacket>>(emptyList())
    override val packets: StateFlow<List<EmergencyPacket>> = _packets.asStateFlow()
    
    private val forwardedPacketIds = mutableSetOf<String>()

    override fun addPacket(packet: EmergencyPacket): Boolean {
        val current = _packets.value
        // Duplicate detection based on packetId
        if (current.any { it.packetId.contentEquals(packet.packetId) }) {
            return false
        }
        _packets.update { it + packet }
        return true
    }

    override fun markAsForwarded(packetId: ByteArray) {
        forwardedPacketIds.add(toHex(packetId))
    }

    override fun isForwarded(packetId: ByteArray): Boolean {
        return forwardedPacketIds.contains(toHex(packetId))
    }

    override fun clear() {
        _packets.value = emptyList()
        forwardedPacketIds.clear()
    }

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}
