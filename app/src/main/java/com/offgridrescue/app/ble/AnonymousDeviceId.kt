package com.offgridrescue.app.ble

import java.security.SecureRandom

object AnonymousDeviceId {
    const val BYTE_COUNT = 8

    fun generate(): ByteArray {
        val bytes = ByteArray(BYTE_COUNT)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun toHex(id: ByteArray): String =
        id.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}
