package com.offgridrescue.app.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymousDeviceIdTest {
    @Test
    fun generate_returnsEightRandomBytes() {
        val first = AnonymousDeviceId.generate()
        val second = AnonymousDeviceId.generate()
        assertEquals(AnonymousDeviceId.BYTE_COUNT, first.size)
        assertEquals(AnonymousDeviceId.BYTE_COUNT, second.size)
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun toHex_formatsLowercaseWithoutSeparators() {
        val id = byteArrayOf(0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F, 0x60, 0x7F)
        assertEquals("0a1b2c3d4e5f607f", AnonymousDeviceId.toHex(id))
    }
}
