package com.offgridrescue.app.ble

import android.bluetooth.le.AdvertiseCallback
import org.junit.Assert.assertTrue
import org.junit.Test

class BleAdvertiseErrorsTest {
    @Test
    fun message_explainsKnownAndroidErrorCodes() {
        val message = BleAdvertiseErrors.message(
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
        )
        assertTrue(message.contains("not supported"))
    }
}
