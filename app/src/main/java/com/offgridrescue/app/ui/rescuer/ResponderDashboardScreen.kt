package com.offgridrescue.app.ui.rescuer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgridrescue.app.domain.*
import com.offgridrescue.app.ui.components.SemanticStatusIndicator
import com.offgridrescue.app.ui.components.SemanticStatusType
import com.offgridrescue.app.ui.components.HeatmapView
import com.offgridrescue.app.ui.emergency.CloudConnectionStatus
import java.time.Duration
import java.time.Instant

@Composable
fun ResponderDashboardRoute(
    viewModel: ResponderDashboardViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startDashboard()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDashboard()
        }
    }

    ResponderDashboardScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponderDashboardScreen(
    uiState: ResponderUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Responder Dashboard") },
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
            // 1. Connection & Authorization Status
            DashboardHeader(uiState)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Heatmap
            HeatmapView(
                nodes = uiState.heatmapNodes,
                modifier = Modifier.height(250.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Incident List
            Text(
                text = "Active Incidents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (uiState.incidents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active incidents detected.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.incidents) { incident ->
                        IncidentCard(incident)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(uiState: ResponderUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SemanticStatusIndicator(
                    type = if (uiState.isOnline) SemanticStatusType.HEALTHY else SemanticStatusType.ERROR,
                    label = "Net: ${if (uiState.isOnline) "Online" else "Offline"}"
                )
                SemanticStatusIndicator(
                    type = when(uiState.cloudConnectionStatus) {
                        CloudConnectionStatus.CONNECTED -> SemanticStatusType.HEALTHY
                        CloudConnectionStatus.DISCONNECTED -> SemanticStatusType.ERROR
                        CloudConnectionStatus.UNKNOWN -> SemanticStatusType.WARNING
                    },
                    label = "Cloud: ${uiState.cloudConnectionStatus.displayName}"
                )
                SemanticStatusIndicator(
                    type = if (uiState.isAuthorized) SemanticStatusType.HEALTHY else SemanticStatusType.ERROR,
                    label = if (uiState.isAuthorized) "Authorized" else "Unauthorized"
                )
            }
            
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.isAuthorized) "✓ AUTHORIZED RESPONDER" else "! UNAUTHORIZED ACCESS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isAuthorized) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
                if (!uiState.isAuthorized) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                }
            }

            if (!uiState.isAuthorized) {
                Text(
                    text = "Cloud incident data is restricted to authorized agencies. Local BLE/Mesh discovery remains active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun IncidentCard(incident: UnifiedIncident) {
    val borderColor = when (incident.priority) {
        IncidentPriority.URGENT -> Color.Red
        IncidentPriority.NEEDS_HELP -> Color(0xFFF57C00)
        IncidentPriority.SAFE -> Color(0xFF388E3C)
        IncidentPriority.STALE -> Color.Gray
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${incident.sourceDeviceId.take(12)}...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Badge(containerColor = borderColor) {
                    Text(incident.priority.name, color = Color.White)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Metadata
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Type: ${incident.sosType?.displayName ?: "Unknown"}", fontSize = 14.sp)
                Text("Bat: ${incident.batteryLevel ?: "?"}%", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status
            Text(
                text = "Check-in: ${incident.checkInStatus?.displayName ?: "No status"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (incident.checkInStatus == CheckInStatus.URGENT) Color.Red else Color.Unspecified
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Proximity (Live BLE)
            if (incident.localProximity != null) {
                Text(
                    text = "LIVE BLE: ${incident.localProximity.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Location (Cloud GPS)
            if (incident.lastKnownLat != null && incident.lastKnownLng != null) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "LAST KNOWN GPS (Cloud)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${incident.lastKnownLat}, ${incident.lastKnownLng}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Acc: ${incident.gpsAccuracy}m | ${incident.gpsTimestamp?.let { 
                            Duration.between(it, Instant.now()).seconds 
                        } ?: "?"}s ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            } else {
                Text(
                    text = "No Cloud GPS data available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
