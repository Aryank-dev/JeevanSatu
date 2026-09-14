package com.offgridrescue.app.ble

import com.offgridrescue.app.domain.EmergencyPacket

/**
 * Structured BLE manufacturer payload.
 * Fits in a legacy 31-byte advertise packet:
 * - Flags (3 bytes)
 * - Manufacturer Data Header (4 bytes)
 * - APP_MARKER (2 bytes)
 * - EmergencyPacket (21 bytes)
 * Total: 30 bytes
 */
object EmergencyAdvertisePayload {
    const val MANUFACTURER_ID = 0xFFFF
    val APP_MARKER: ByteArray = byteArrayOf(0x0F.toByte(), 0x52.toByte())

    fun build(packet: EmergencyPacket): ByteArray {
        val serialized = PacketSerializer.serialize(packet)
        return APP_MARKER + serialized
    }
}
