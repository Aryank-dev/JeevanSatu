package com.offgridrescue.app.ui.home

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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.offgridrescue.app.domain.AlertSeverity
import com.offgridrescue.app.domain.OfficialAlert
import com.offgridrescue.app.ui.emergency.EmergencyUiState
import com.offgridrescue.app.ui.theme.JeevanSetuTheme

@Composable
fun HomeRoute(
    emergencyUiState: EmergencyUiState,
    onEmergencyClick: () -> Unit,
    onVictimClick: () -> Unit,
    onRescuerClick: () -> Unit,
    onResponderDashboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val homeUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiState = homeUiState.copy(
        currentMode = emergencyUiState.currentMode,
        emergencyStatus = emergencyUiState.emergencyStatus,
        bleStatus = emergencyUiState.bleAdvertisingState.displayName
    )
    HomeScreen(
        uiState = uiState,
        onEmergencyClick = onEmergencyClick,
        onVictimClick = onVictimClick,
        onRescuerClick = onRescuerClick,
        onResponderDashboardClick = onResponderDashboardClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEmergencyClick: () -> Unit,
    onVictimClick: () -> Unit,
    onRescuerClick: () -> Unit,
    onResponderDashboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(uiState.appName) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.activeAlerts.isNotEmpty()) {
                OfficialAlertBanner(alerts = uiState.activeAlerts)
            }

            StatusCard(label = "Current mode", value = uiState.currentMode.displayName)
            StatusCard(label = "Emergency status", value = uiState.emergencyStatus.displayName)
            StatusCard(label = "Battery", value = "${uiState.batteryPercent}%")
            StatusCard(label = "BLE status", value = uiState.bleStatus)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Modes",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onEmergencyClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Emergency Mode")
            }
            Button(
                onClick = onVictimClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Victim Mode")
            }
            Button(
                onClick = onRescuerClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rescuer Mode")
            }
            Button(
                onClick = onResponderDashboardClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Responder Dashboard (Auth)")
            }
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Settings")
            }
        }
    }
}

@Composable
private fun OfficialAlertBanner(alerts: List<OfficialAlert>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Official Alerts (Demo) • ${alerts.size} active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alerts.forEach { alert ->
                        AlertCard(alert)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: OfficialAlert) {
    val bgColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = alert.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
                Text(
                    text = "Agency: ${alert.agency}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    JeevanSetuTheme {
        HomeScreen(
            uiState = HomeUiState(batteryPercent = 84),
            onEmergencyClick = {},
            onVictimClick = {},
            onRescuerClick = {},
            onResponderDashboardClick = {},
            onSettingsClick = {}
        )
    }
}
