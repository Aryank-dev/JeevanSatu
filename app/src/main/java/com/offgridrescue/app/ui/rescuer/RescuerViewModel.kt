package com.offgridrescue.app.ui.rescuer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.offgridrescue.app.ble.AppPermissions
import com.offgridrescue.app.ble.EmergencyBleAdvertiser
import com.offgridrescue.app.ble.EmergencyBleScanner
import com.offgridrescue.app.data.InMemoryEmergencyPacketRepository
import com.offgridrescue.app.domain.DeviceProximity
import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.PacketValidator
import com.offgridrescue.app.domain.ProximityAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.offgridrescue.app.domain.HeatmapNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class RescuerViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "RescuerViewModel"
    private val scanner = EmergencyBleScanner(application.applicationContext)
    private val relayAdvertiser = EmergencyBleAdvertiser(application.applicationContext)
    private val analyzer = ProximityAnalyzer()
    private val packetRepository = InMemoryEmergencyPacketRepository()

    private val _uiState = MutableStateFlow(RescuerUiState())
    val uiState: StateFlow<RescuerUiState> = _uiState.asStateFlow()

    private var staleCleanupJob: Job? = null
    private val STALE_TIMEOUT_SECONDS = 20L

    init {
        // Synchronize relay packets from repository to UI state
        packetRepository.packets
            .onEach { packets ->
                _uiState.update { it.copy(relayPackets = packets) }
            }
            .launchIn(viewModelScope)

        // Start stale cleanup for rescuer view
        startStaleCleanupJob()
    }

    fun startScanning() {
        Log.d(tag, "startScanning() called")
        val context = getApplication<Application>().applicationContext

        if (!scanner.isSupported()) {
            _uiState.update { it.copy(error = "BLE not supported on this device") }
            return
        }

        if (!AppPermissions.areGranted(context, AppPermissions.SCAN)) {
            _uiState.update { it.copy(hasPermissions = false) }
            return
        }

        if (!scanner.isEnabled()) {
            _uiState.update { it.copy(isBluetoothEnabled = false) }
            return
        }

        _uiState.update { it.copy(isScanning = true, error = null, hasPermissions = true, isBluetoothEnabled = true) }

        scanner.start(object : EmergencyBleScanner.Listener {
            override fun onPacketDetected(packet: EmergencyPacket, rssi: Int) {
                Log.d(tag, "[RELAY-RX] Packet received. ID: ${packet.toHexPacketId()}, Source ID: ${packet.toHexSourceId()}, Hop count: ${packet.hopCount}")
                
                // 1. Validate packet
                if (!PacketValidator.isValid(packet)) {
                    Log.w(tag, "[RELAY-RX] Received invalid/expired packet. Ignoring.")
                    return
                }
                Log.d(tag, "[RELAY-RX] Packet validated.")

                // 2. Process for UI (RSSI tracking) - Step 7 logic remains intact
                val deviceId = packet.toHexSourceId()
                
                _uiState.update { state ->
                    val existing = state.devices.find { it.deviceId == deviceId }
                    val updatedDevice = analyzer.analyze(deviceId, rssi, existing)
                        .copy(latestPacket = packet) 
                    
                    val newDevices = state.devices.toMutableList()
                    val index = newDevices.indexOfFirst { it.deviceId == deviceId }
                    if (index != -1) {
                        newDevices[index] = updatedDevice
                    } else {
                        newDevices.add(updatedDevice)
                    }
                    
                    state.copy(devices = newDevices.sortedBy { it.proximity.ordinal })
                }
                
                // Heatmap Update (Step 13)
                updateHeatmapNodes()
                
                // 3. Store in repository (Relay layer)
                val wasAdded = packetRepository.addPacket(packet)
                if (wasAdded) {
                    Log.i(tag, "[RELAY-STORE] Packet stored. ID: ${packet.toHexPacketId()}")
                    
                    // 4. Forwarding Logic (Step 9B.2)
                    forwardPacketIfEligible(packet)
                } else {
                    Log.d(tag, "[RELAY-STORE] Duplicate packet ignored. ID: ${packet.toHexPacketId()}")
                }
            }

            override fun onError(message: String) {
                _uiState.update { it.copy(error = message, isScanning = false) }
            }
        })
    }

    private fun forwardPacketIfEligible(packet: EmergencyPacket) {
        val packetIdHex = packet.toHexPacketId()

        // Check if already forwarded
        if (packetRepository.isForwarded(packet.packetId)) {
            Log.d(tag, "[RELAY-TX] Duplicate forwarding prevented. ID: $packetIdHex")
            return
        }

        // Check expiration
        if (PacketValidator.isExpired(packet)) {
            Log.w(tag, "[RELAY-TX] Packet rejected: expired. ID: $packetIdHex")
            return
        }

        // Check Hop Count
        if (packet.hopCount >= PacketValidator.MAX_HOPS) {
            Log.w(tag, "[RELAY-TX] Packet rejected: MAX_HOPS reached (${packet.hopCount}). ID: $packetIdHex")
            return
        }

        // Create forwarded copy
        val forwardedPacket = packet.copy(hopCount = packet.hopCount + 1)
        
        Log.i(tag, "[RELAY-TX] Forwarding packet. ID: $packetIdHex, Original hop: ${packet.hopCount}, New hop: ${forwardedPacket.hopCount}")

        // Mark as forwarded in repository BEFORE starting (to prevent race conditions if scan triggers again)
        packetRepository.markAsForwarded(packet.packetId)

        // Start advertising
        relayAdvertiser.start(forwardedPacket, object : EmergencyBleAdvertiser.Listener {
            override fun onStarted() {
                Log.i(tag, "[RELAY-TX] Forwarding started for packet ID: $packetIdHex")
            }

            override fun onFailed(message: String) {
                Log.e(tag, "[RELAY-TX] Forwarding failed: $message")
            }
        })
    }

    private fun startStaleCleanupJob() {
        staleCleanupJob?.cancel()
        staleCleanupJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Check every 5 seconds
                pruneStaleDevices()
            }
        }
    }

    private fun pruneStaleDevices() {
        val now = Instant.now()
        _uiState.update { state ->
            val activeDevices = state.devices.filter {
                Duration.between(it.lastSeen, now).seconds < STALE_TIMEOUT_SECONDS
            }
            if (activeDevices.size != state.devices.size) {
                Log.d(tag, "Pruned ${state.devices.size - activeDevices.size} stale devices.")
                state.copy(devices = activeDevices)
            } else {
                state
            }
        }
        updateHeatmapNodes()
    }

    private fun updateHeatmapNodes() {
        _uiState.update { state ->
            val nodes = state.devices.map { device ->
                HeatmapNode(
                    deviceId = device.deviceId,
                    proximity = device.proximity,
                    signalTrend = device.signalTrend,
                    isDirect = (device.latestPacket?.hopCount ?: 0) == 0,
                    sosType = device.latestPacket?.sosType,
                    batteryLevel = device.latestPacket?.batteryLevel,
                    lastSeen = device.lastSeen
                )
            }
            state.copy(heatmapNodes = nodes)
        }
    }

    fun stopScanning() {
        Log.d(tag, "stopScanning() called")
        scanner.stop()
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stop()
        relayAdvertiser.stop()
    }
}
