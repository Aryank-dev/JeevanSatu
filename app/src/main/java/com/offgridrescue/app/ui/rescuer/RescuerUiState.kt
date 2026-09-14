package com.offgridrescue.app.ui.rescuer

import com.offgridrescue.app.domain.DeviceProximity
import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.HeatmapNode

data class RescuerUiState(
    val devices: List<DeviceProximity> = emptyList(),
    val relayPackets: List<EmergencyPacket> = emptyList(),
    val heatmapNodes: List<HeatmapNode> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null,
    val isBluetoothEnabled: Boolean = true,
    val hasPermissions: Boolean = true
)
