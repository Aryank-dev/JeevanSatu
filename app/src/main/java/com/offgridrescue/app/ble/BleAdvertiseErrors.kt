package com.offgridrescue.app.ble

import android.bluetooth.le.AdvertiseCallback

object BleAdvertiseErrors {
    fun message(errorCode: Int): String = when (errorCode) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
            "Advertise data is too large for this device."
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
            "Too many BLE advertisers are already running."
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED ->
            "Advertising was already started."
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
            "Internal Bluetooth error."
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
            "BLE advertising is not supported on this device."
        else ->
            "Advertising failed (error $errorCode)."
    }
}
