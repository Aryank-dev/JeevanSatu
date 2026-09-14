package com.offgridrescue.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class EmergencyBleAdvertiser(private val context: Context) {
    private val tag = "EmergencyBleAdvertiser"
    private val started = AtomicBoolean(false)
    private var advertiser: BluetoothLeAdvertiser? = null
    private var callback: AdvertiseCallback? = null

    fun hasBleHardware(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothManager()?.adapter != null

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        if (!AppPermissions.areGranted(context, AppPermissions.ADVERTISE)) return false
        return bluetoothManager()?.adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun start(packet: com.offgridrescue.app.domain.EmergencyPacket, listener: Listener) {
        Log.d(tag, "Attempting to start BLE advertising structured packet...")
        if (!AppPermissions.areGranted(context, AppPermissions.ADVERTISE)) {
            Log.e(tag, "Start failed: Permissions not granted.")
            listener.onFailed("Bluetooth advertising permission is not granted.")
            return
        }
        if (!hasBleHardware()) {
            Log.e(tag, "Start failed: BLE not supported by hardware.")
            listener.onFailed("This device does not support Bluetooth Low Energy.")
            return
        }
        val adapter = bluetoothManager()?.adapter
        if (adapter == null) {
            Log.e(tag, "Start failed: Bluetooth adapter is null.")
            listener.onFailed("This device does not have a Bluetooth adapter.")
            return
        }
        if (!adapter.isEnabled) {
            Log.e(tag, "Start failed: Bluetooth is disabled.")
            listener.onFailed("Bluetooth is turned off.")
            return
        }
        val leAdvertiser = adapter.bluetoothLeAdvertiser
        if (leAdvertiser == null) {
            Log.e(tag, "Start failed: BluetoothLeAdvertiser is null (check if airplane mode is on).")
            listener.onFailed(
                "BLE advertising is not available. Many emulators cannot advertise."
            )
            return
        }

        if (started.get()) {
            Log.w(tag, "Already advertising. Stopping previous session first.")
            stop()
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val payload = EmergencyAdvertisePayload.build(packet)
        Log.d(tag, "[BLE-TX] Preparing advertisement payload.")
        Log.d(tag, "[BLE-TX] Packet ID: ${packet.toHexPacketId()}")
        Log.d(tag, "[BLE-TX] Serialized Packet size: 21 bytes")
        Log.d(tag, "[BLE-TX] Total Payload size (with marker): ${payload.size} bytes")

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(
                EmergencyAdvertisePayload.MANUFACTURER_ID,
                payload
            )
            .build()

        val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i(tag, "BLE advertising started successfully.")
                started.set(true)
                listener.onStarted()
            }

            override fun onStartFailure(errorCode: Int) {
                val errorMsg = BleAdvertiseErrors.message(errorCode)
                Log.e(tag, "BLE advertising failed to start. Error code: $errorCode ($errorMsg)")
                started.set(false)
                advertiser = null
                callback = null
                listener.onFailed(errorMsg)
            }
        }

        this.advertiser = leAdvertiser
        this.callback = advertiseCallback
        try {
            Log.d(tag, "Calling native startAdvertising API...")
            leAdvertiser.startAdvertising(settings, data, advertiseCallback)
        } catch (security: SecurityException) {
            Log.e(tag, "SecurityException while starting advertising: ${security.message}")
            started.set(false)
            this.advertiser = null
            this.callback = null
            listener.onFailed("Android blocked advertising: ${security.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        Log.d(tag, "Stopping BLE advertising...")
        val activeAdvertiser = advertiser
        val activeCallback = callback
        started.set(false)
        advertiser = null
        callback = null
        if (activeAdvertiser == null || activeCallback == null) {
            Log.d(tag, "Nothing to stop; advertiser was not active.")
            return
        }
        if (!AppPermissions.areGranted(context, AppPermissions.ADVERTISE)) {
            Log.w(tag, "Cannot call stopAdvertising: Permissions were revoked.")
            return
        }
        try {
            activeAdvertiser.stopAdvertising(activeCallback)
            Log.i(tag, "BLE advertising stopped.")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping BLE advertising: ${e.message}")
        }
    }

    private fun bluetoothManager(): BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    interface Listener {
        fun onStarted()
        fun onFailed(message: String)
    }
}
