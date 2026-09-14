package com.offgridrescue.app.ui.emergency

import android.location.Location
import com.offgridrescue.app.ble.BleAdvertisingState
import com.offgridrescue.app.domain.*

enum class GpsStatus(val displayName: String) {
    ACTIVE("Active"),
    UNAVAILABLE("Unavailable"),
    PERMISSION_DENIED("Permission Denied")
}

enum class CloudSyncStatus(val displayName: String) {
    IDLE("Ready"),
    SYNCING("Syncing..."),
    SYNCED("Synced"),
    PENDING("Pending"),
    ERROR("Sync Error"),
    AUTH_ERROR("Auth Error")
}

enum class CloudConnectionStatus(val displayName: String) {
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    UNKNOWN("Unknown")
}

data class EmergencyUiState(
    val currentMode: AppMode = AppMode.STANDBY,
    val emergencyStatus: EmergencyStatus = EmergencyStatus.INACTIVE,
    val bleAdvertisingState: BleAdvertisingState = BleAdvertisingState.NOT_ADVERTISING,
    val anonymousIdHex: String? = null,
    val selectedSosType: SosType = SosType.GENERAL,
    val batteryLevel: Int? = null,
    val errorMessage: String? = null,
    val nearbyDevices: List<DeviceProximity> = emptyList(),
    val isNearbyAwarenessEnabled: Boolean = true,
    val nearbyAwarenessError: String? = null,
    val lastKnownLocation: Location? = null,
    val gpsStatus: GpsStatus = GpsStatus.UNAVAILABLE,
    val locationSyncStatus: CloudSyncStatus = CloudSyncStatus.IDLE,
    val checkInSyncStatus: CloudSyncStatus = CloudSyncStatus.IDLE,
    val locationSyncError: String? = null,
    val checkInSyncError: String? = null,
    val networkStatus: NetworkStatus = NetworkStatus.ONLINE,
    val cloudConnectionStatus: CloudConnectionStatus = CloudConnectionStatus.UNKNOWN,
    val currentCheckInStatus: CheckInStatus? = null,
    val activeAlerts: List<OfficialAlert> = emptyList()
)
