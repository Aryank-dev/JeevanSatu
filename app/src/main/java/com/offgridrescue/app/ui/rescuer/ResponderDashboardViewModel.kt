package com.offgridrescue.app.ui.rescuer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offgridrescue.app.ble.AppPermissions
import com.offgridrescue.app.ble.EmergencyBleScanner
import com.offgridrescue.app.data.location.ResponderRepository
import com.offgridrescue.app.data.location.FirebaseResponderRepository
import com.offgridrescue.app.data.location.CloudIncidentUpdate
import com.offgridrescue.app.device.NetworkMonitor
import com.offgridrescue.app.domain.*
import com.offgridrescue.app.ui.emergency.CloudConnectionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

class ResponderDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ResponderDashboardVM"
    private val scanner = EmergencyBleScanner(application.applicationContext)
    private val analyzer = ProximityAnalyzer()
    private val responderRepo: ResponderRepository = FirebaseResponderRepository()
    private val networkMonitor = NetworkMonitor(application.applicationContext)

    private val _uiState = MutableStateFlow(ResponderUiState())
    val uiState: StateFlow<ResponderUiState> = _uiState.asStateFlow()

    private val localDevices = MutableStateFlow<Map<String, DeviceProximity>>(emptyMap())
    private val cloudUpdates = MutableStateFlow<Map<String, CloudIncidentUpdate>>(emptyMap())

    init {
        // 1. Authorization check
        responderRepo.isAuthorized()
            .onEach { authorized ->
                _uiState.update { it.copy(isAuthorized = authorized) }
                if (authorized) {
                    observeCloudData()
                }
            }
            .launchIn(viewModelScope)

        // 2. Network monitoring
        networkMonitor.status
            .onEach { status ->
                _uiState.update { 
                    it.copy(
                        isOnline = status == NetworkStatus.ONLINE,
                        cloudConnectionStatus = if (status == NetworkStatus.ONLINE) 
                            CloudConnectionStatus.CONNECTED else CloudConnectionStatus.DISCONNECTED
                    )
                }
            }
            .launchIn(viewModelScope)

        // 3. Merge logic
        combine(localDevices, cloudUpdates) { local, cloud ->
            mergeIncidents(local, cloud)
        }.onEach { merged ->
            _uiState.update { state ->
                state.copy(
                    incidents = merged.sortedBy { it.priority.ordinal },
                    heatmapNodes = merged.mapNotNull { it.toHeatmapNode() }
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeCloudData() {
        responderRepo.observeAllIncidents()
            .onEach { updates ->
                cloudUpdates.value = updates.associateBy { it.sourceDeviceId }
            }
            .launchIn(viewModelScope)
    }

    fun startDashboard() {
        if (!AppPermissions.areGranted(getApplication(), AppPermissions.SCAN)) {
            _uiState.update { it.copy(error = "Permission needed") }
            return
        }

        _uiState.update { it.copy(isScanning = true) }
        scanner.start(object : EmergencyBleScanner.Listener {
            override fun onPacketDetected(packet: EmergencyPacket, rssi: Int) {
                val sId = packet.toHexSourceId()
                val existing = localDevices.value[sId]
                val updated = analyzer.analyze(sId, rssi, existing).copy(latestPacket = packet)
                
                localDevices.update { it + (sId to updated) }
            }

            override fun onError(message: String) {
                _uiState.update { it.copy(error = message, isScanning = false) }
            }
        })
    }

    private fun mergeIncidents(
        local: Map<String, DeviceProximity>,
        cloud: Map<String, CloudIncidentUpdate>
    ): List<UnifiedIncident> {
        val allIds = local.keys + cloud.keys
        return allIds.map { id ->
            val l = local[id]
            val c = cloud[id]
            
            UnifiedIncident(
                sourceDeviceId = id,
                sosType = l?.latestPacket?.sosType ?: c?.sosType?.let { SosType.valueOf(it) },
                batteryLevel = l?.latestPacket?.batteryLevel,
                checkInStatus = c?.checkInStatus?.let { CheckInStatus.valueOf(it) },
                lastSeenLocal = l?.lastSeen,
                lastSeenCloud = c?.timestamp,
                lastKnownLat = c?.lat,
                lastKnownLng = c?.lng,
                gpsAccuracy = c?.accuracy,
                gpsTimestamp = c?.timestamp,
                localProximity = l?.proximity,
                localTrend = l?.signalTrend,
                hopCount = l?.latestPacket?.hopCount,
                isDirect = l?.latestPacket?.let { it.hopCount == 0 }
            )
        }
    }

    fun stopDashboard() {
        scanner.stop()
        _uiState.update { it.copy(isScanning = false) }
    }

    private fun UnifiedIncident.toHeatmapNode(): HeatmapNode? {
        if (localProximity == null) return null
        return HeatmapNode(
            deviceId = sourceDeviceId,
            proximity = localProximity,
            signalTrend = localTrend ?: SignalTrend.UNKNOWN,
            isDirect = isDirect ?: true,
            sosType = sosType,
            batteryLevel = batteryLevel,
            lastSeen = lastSeenLocal ?: Instant.now()
        )
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stop()
    }
}
