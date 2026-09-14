package com.offgridrescue.app.ui.rescuer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgridrescue.app.ble.AppPermissions
import com.offgridrescue.app.ui.components.HeatmapView
import com.offgridrescue.app.domain.DeviceProximity
import com.offgridrescue.app.domain.EmergencyPacket
import com.offgridrescue.app.domain.ProximityLevel
import com.offgridrescue.app.domain.SignalTrend
import java.time.Duration
import java.time.Instant

@Composable
fun RescuerModeRoute(
    viewModel: RescuerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.startScanning()
    }

    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }

    RescuerModeScreen(
        uiState = uiState,
        onRequestPermissions = {
            permissionLauncher.launch(AppPermissions.SCAN)
        },
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescuerModeScreen(
    uiState: RescuerUiState,
    onRequestPermissions: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rescuer Mode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!uiState.hasPermissions) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ErrorMessage("Bluetooth scanning permissions required.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRequestPermissions) {
                        Text("Grant Permissions")
                    }
                }
            } else if (!uiState.isBluetoothEnabled) {
                ErrorMessage("Please turn on Bluetooth to scan.")
            } else if (uiState.error != null) {
                ErrorMessage(uiState.error)
            } else {
                if (uiState.isScanning) {
                    if (uiState.devices.isEmpty()) {
                        ScanningRadarView()
                    } else {
                        HeatmapView(nodes = uiState.heatmapNodes)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = "Scanner Stopped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                RelayStorageSection(uiState.relayPackets)

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.devices.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.devices) { device ->
                            DeviceCard(device)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningRadarView() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { this.alpha = alpha },
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Scanning for nearby emergency devices",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "BLE mesh discovery is active.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RelayStorageSection(packets: List<EmergencyPacket>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Relay Storage: ${packets.size} Packets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (packets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                packets.forEach { packet ->
                    RelayPacketItem(packet)
                    if (packet != packets.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun RelayPacketItem(packet: EmergencyPacket) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Packet: ${packet.toHexPacketId()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("Hop: ${packet.hopCount}", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Source: ${packet.toHexSourceId()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            val remaining = packet.ttlMinutes - Duration.between(packet.createdAt, Instant.now()).toMinutes()
            Text("Expires: ${remaining}m", style = MaterialTheme.typography.bodySmall, color = if (remaining < 5) Color.Red else Color.Gray)
        }
    }
}

@Composable
fun DeviceCard(device: DeviceProximity) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "EMERGENCY DEVICE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            device.latestPacket?.let { packet ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Type: ${packet.sosType.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Battery: ${packet.batteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (packet.batteryLevel < 20) Color.Red else Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            InfoRow(label = "Device ID:", value = device.deviceId)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Proximity:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = device.proximity.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = proximityColor(device.proximity)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("RSSI:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${device.rawRssi} dBm", style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Smoothed:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("%.1f dBm".format(device.smoothedRssi), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                label = "Signal Trend:",
                value = "${device.signalTrend.displayName} ${device.signalTrend.symbol}",
                valueColor = trendColor(device.signalTrend)
            )

            Spacer(modifier = Modifier.height(4.dp))

            val secondsAgo = Duration.between(device.lastSeen, Instant.now()).seconds
            Text(
                text = "Last Seen: $secondsAgo seconds ago",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun proximityColor(level: ProximityLevel): Color = when (level) {
    ProximityLevel.VERY_CLOSE -> Color(0xFFD32F2F) // Red
    ProximityLevel.NEARBY -> Color(0xFFF57C00) // Orange
    ProximityLevel.FAR -> Color(0xFF388E3C) // Green
    ProximityLevel.UNKNOWN -> Color.Gray
}

private fun trendColor(trend: SignalTrend): Color = when (trend) {
    SignalTrend.STRONGER -> Color(0xFF388E3C)
    SignalTrend.WEAKER -> Color(0xFFD32F2F)
    else -> Color.Unspecified
}
