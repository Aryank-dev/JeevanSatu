package com.offgridrescue.app.ui.victim

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgridrescue.app.ble.AppPermissions
import com.offgridrescue.app.ble.BleAdvertisingState
import com.offgridrescue.app.domain.EmergencyStatus
import com.offgridrescue.app.ui.emergency.CloudSyncStatus
import com.offgridrescue.app.ui.emergency.EmergencyUiState
import com.offgridrescue.app.ui.emergency.EmergencyViewModel
import com.offgridrescue.app.ui.emergency.GpsStatus
import com.offgridrescue.app.domain.NetworkStatus
import com.offgridrescue.app.ui.components.SemanticStatusType
import com.offgridrescue.app.ui.components.StatusTile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.offgridrescue.app.domain.DeviceProximity
import com.offgridrescue.app.domain.ProximityLevel
import com.offgridrescue.app.domain.SignalTrend
import com.offgridrescue.app.domain.SosType
import com.offgridrescue.app.domain.CheckInStatus
import com.offgridrescue.app.domain.OfficialAlert
import com.offgridrescue.app.domain.AlertSeverity

@Composable
fun VictimModeRoute(
    viewModel: EmergencyViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.startEmergency()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Start regardless of location result (Mesh Only fallback)
        viewModel.startEmergency()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.startEmergency()
    }

    VictimModeScreen(
        uiState = uiState,
        onStartEmergency = { 
            val missingLocation = AppPermissions.missing(context, AppPermissions.LOCATION)
            if (missingLocation.isNotEmpty()) {
                locationPermissionLauncher.launch(AppPermissions.LOCATION)
            } else {
                viewModel.startEmergency()
            }
        },
        onStopEmergency = { viewModel.endEmergency() },
        onSosTypeSelected = { viewModel.onSosTypeSelected(it) },
        onCheckInSelected = { viewModel.onCheckInSelected(it) },
        onRequestPermissions = {
            blePermissionLauncher.launch(AppPermissions.ADVERTISE)
        },
        onEnableBluetooth = {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        },
        onBackClick = onBackClick,
        showEnableBluetooth = uiState.bleAdvertisingState == BleAdvertisingState.BLUETOOTH_OFF &&
            AppPermissions.areGranted(context, AppPermissions.ADVERTISE),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VictimModeScreen(
    uiState: EmergencyUiState,
    onStartEmergency: () -> Unit,
    onStopEmergency: () -> Unit,
    onSosTypeSelected: (SosType) -> Unit,
    onCheckInSelected: (CheckInStatus) -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onBackClick: () -> Unit,
    showEnableBluetooth: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Victim Mode") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrivacyNotice()

            if (uiState.emergencyStatus == EmergencyStatus.INACTIVE) {
                Text(
                    text = "Select Emergency Type:",
                    style = MaterialTheme.typography.titleMedium
                )
                SosTypeSelector(
                    selectedType = uiState.selectedSosType,
                    onTypeSelected = onSosTypeSelected
                )
            }

            StatusCard(
                label = "BLE Status",
                value = uiState.bleAdvertisingState.displayName,
                isError = uiState.bleAdvertisingState == BleAdvertisingState.FAILED ||
                        uiState.bleAdvertisingState == BleAdvertisingState.NOT_SUPPORTED
            )

            if (uiState.emergencyStatus == EmergencyStatus.ACTIVE) {
                if (uiState.activeAlerts.isNotEmpty()) {
                    OfficialAlertSection(alerts = uiState.activeAlerts)
                }

                OperationalStatusGrid(uiState)

                CheckInStatusSection(
                    currentStatus = uiState.currentCheckInStatus,
                    onStatusSelected = onCheckInSelected
                )

                StatusCard(
                    label = "Emergency Category",
                    value = uiState.selectedSosType.displayName,
                    isHighlight = true
                )

                StatusCard(
                    label = "Anonymous Emergency ID",
                    value = uiState.anonymousIdHex ?: "Initializing...",
                    isHighlight = true
                )
                
                uiState.batteryLevel?.let {
                    StatusCard(
                        label = "Battery Snapshot",
                        value = "$it%",
                        isHighlight = false
                    )
                }

                NearbyAwarenessSection(
                    devices = uiState.nearbyDevices,
                    error = uiState.nearbyAwarenessError
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.emergencyStatus == EmergencyStatus.INACTIVE) {
                Button(
                    onClick = onStartEmergency,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Emergency Broadcast")
                }
            } else {
                when (uiState.bleAdvertisingState) {
                    BleAdvertisingState.PERMISSION_NEEDED -> {
                        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant Bluetooth Permissions")
                        }
                    }
                    BleAdvertisingState.BLUETOOTH_OFF -> {
                        if (showEnableBluetooth) {
                            Button(onClick = onEnableBluetooth, modifier = Modifier.fillMaxWidth()) {
                                Text("Turn on Bluetooth")
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStopEmergency,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop Emergency Broadcast")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
        }
    }
}

@Composable
private fun PrivacyNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Anonymous Broadcast — BLE packets do not contain PII or raw GPS coordinates.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OperationalStatusGrid(uiState: EmergencyUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTile(
                title = "GPS",
                statusText = uiState.gpsStatus.displayName,
                statusType = when (uiState.gpsStatus) {
                    GpsStatus.ACTIVE -> SemanticStatusType.HEALTHY
                    GpsStatus.PERMISSION_DENIED -> SemanticStatusType.ERROR
                    else -> SemanticStatusType.WARNING
                },
                modifier = Modifier.weight(1f)
            )
            StatusTile(
                title = "Network",
                statusText = uiState.networkStatus.displayName,
                statusType = when (uiState.networkStatus) {
                    NetworkStatus.ONLINE -> SemanticStatusType.HEALTHY
                    else -> SemanticStatusType.ERROR
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTile(
                title = "Loc Sync",
                statusText = uiState.locationSyncStatus.displayName,
                statusType = when (uiState.locationSyncStatus) {
                    CloudSyncStatus.SYNCED -> SemanticStatusType.HEALTHY
                    CloudSyncStatus.ERROR, CloudSyncStatus.AUTH_ERROR -> SemanticStatusType.ERROR
                    else -> SemanticStatusType.WARNING
                },
                subText = uiState.locationSyncError,
                modifier = Modifier.weight(1f)
            )
            StatusTile(
                title = "Check-in",
                statusText = uiState.checkInSyncStatus.displayName,
                statusType = when (uiState.checkInSyncStatus) {
                    CloudSyncStatus.SYNCED -> SemanticStatusType.HEALTHY
                    CloudSyncStatus.ERROR, CloudSyncStatus.AUTH_ERROR -> SemanticStatusType.ERROR
                    else -> SemanticStatusType.WARNING
                },
                subText = uiState.checkInSyncError,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OfficialAlertSection(alerts: List<OfficialAlert>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Relevant Alerts (Demo)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        alerts.forEach { alert ->
            val color = when (alert.severity) {
                AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                AlertSeverity.WARNING -> Color(0xFFF57C00)
                AlertSeverity.INFO -> MaterialTheme.colorScheme.primary
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = alert.displayTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInStatusSection(
    currentStatus: CheckInStatus?,
    onStatusSelected: (CheckInStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Emergency Check-in",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CheckInStatus.values().forEach { status ->
                    val isSelected = status == currentStatus
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStatusSelected(status) },
                        label = { Text(status.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (currentStatus != null) {
                Text(
                    text = "Current Status: ${currentStatus.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun NearbyAwarenessSection(
    devices: List<DeviceProximity>,
    error: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nearby Awareness (Mesh)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "YOU (Broadcasting...)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (devices.isEmpty()) {
                    Text(
                        text = "Searching for nearby participating devices...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    devices.forEach { device ->
                        NearbyDeviceItem(device)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Note: Proximity is estimated based on signal strength and can be affected by physical barriers.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NearbyDeviceItem(device: DeviceProximity) {
    val packet = device.latestPacket
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(end = 4.dp)
                        .graphicsLayer { clip = true; shape = androidx.compose.foundation.shape.CircleShape }
                        .background(proximityColor(device.proximity))
                )
                Text(
                    text = device.proximity.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = proximityColor(device.proximity)
                )
            }
            
            val isDirect = (packet?.hopCount ?: 0) == 0
            Text(
                text = if (isDirect) "DIRECT" else "RELAYED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDirect) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ID: ${device.deviceId.take(8)}...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            if (packet != null) {
                Text(
                    text = "${packet.sosType.displayName} | Bat: ${packet.batteryLevel}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun proximityColor(level: ProximityLevel): Color = when (level) {
    ProximityLevel.VERY_CLOSE -> Color(0xFFD32F2F) // Red
    ProximityLevel.NEARBY -> Color(0xFFF57C00) // Orange
    ProximityLevel.FAR -> Color(0xFF388E3C) // Green
    ProximityLevel.UNKNOWN -> Color.Gray
}

@Composable
private fun SosTypeSelector(
    selectedType: SosType,
    onTypeSelected: (SosType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SosType.values().toList().chunked(3).forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) },
                        label = { Text(type.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    isError: Boolean = false,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isError) MaterialTheme.colorScheme.error 
                        else if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
