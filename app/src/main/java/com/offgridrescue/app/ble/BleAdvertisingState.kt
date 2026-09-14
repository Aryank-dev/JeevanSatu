package com.offgridrescue.app.ble

enum class BleAdvertisingState(val displayName: String) {
    NOT_ADVERTISING("BLE Advertising: OFF"),
    PERMISSION_NEEDED("BLE Advertising: Permission Required"),
    BLUETOOTH_OFF("BLE Advertising: OFF"),
    NOT_SUPPORTED("BLE Advertising: Unsupported"),
    STARTING("BLE Advertising: Starting..."),
    ADVERTISING("BLE Advertising: ON"),
    FAILED("BLE Advertising: Error")
}
