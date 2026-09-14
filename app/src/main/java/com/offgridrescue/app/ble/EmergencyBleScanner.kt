package com.offgridrescue.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

class EmergencyBleScanner(private val context: Context) {
    private val tag = "EmergencyBleScanner"
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null

    fun isSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothManager()?.adapter != null

    @SuppressLint("MissingPermission")
    fun isEnabled(): Boolean = bluetoothManager()?.adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun start(listener: Listener) {
        Log.d(tag, "Starting BLE scan...")
        if (!AppPermissions.areGranted(context, AppPermissions.SCAN)) {
            listener.onError("Bluetooth scan permission not granted.")
            return
        }

        val adapter = bluetoothManager()?.adapter
        if (adapter == null || !adapter.isEnabled) {
            listener.onError("Bluetooth is disabled.")
            return
        }

        val leScanner = adapter.bluetoothLeScanner
        if (leScanner == null) {
            listener.onError("BLE scanning not available.")
            return
        }

        val filter = ScanFilter.Builder()
            .setManufacturerData(EmergencyAdvertisePayload.MANUFACTURER_ID, null)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getManufacturerSpecificData(EmergencyAdvertisePayload.MANUFACTURER_ID)
                if (data != null) {
                    Log.v(tag, "[BLE-RX] Manufacturer data detected. Size: ${data.size} bytes")
                    
                    if (data.size >= EmergencyAdvertisePayload.APP_MARKER.size) {
                        val markerMatches = data.sliceArray(0 until EmergencyAdvertisePayload.APP_MARKER.size)
                            .contentEquals(EmergencyAdvertisePayload.APP_MARKER)
                        
                        if (markerMatches) {
                            Log.d(tag, "[BLE-RX] JeevanSetu marker valid.")
                            val packetBytes = data.sliceArray(EmergencyAdvertisePayload.APP_MARKER.size until data.size)
                            
                            try {
                                val packet = PacketSerializer.deserialize(packetBytes)
                                if (packet != null) {
                                    Log.d(tag, "[BLE-RX] Packet deserialized. ID: ${packet.toHexPacketId()}")
                                    listener.onPacketDetected(packet, result.rssi)
                                } else {
                                    Log.w(tag, "[BLE-RX] Deserialization returned null. Malformed packet?")
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "[BLE-RX] Critical error during packet deserialization: ${e.message}")
                            }
                        }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "Scan failed with error code: $errorCode")
                listener.onError("Scan failed: $errorCode")
            }
        }

        this.scanner = leScanner
        this.callback = scanCallback
        leScanner.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        Log.d(tag, "Stopping BLE scan...")
        val activeScanner = scanner
        val activeCallback = callback
        if (activeScanner != null && activeCallback != null) {
            try {
                activeScanner.stopScan(activeCallback)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping scan: ${e.message}")
            }
        }
        scanner = null
        callback = null
    }

    private fun bluetoothManager(): BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    interface Listener {
        fun onPacketDetected(packet: com.offgridrescue.app.domain.EmergencyPacket, rssi: Int)
        fun onError(message: String)
    }
}
