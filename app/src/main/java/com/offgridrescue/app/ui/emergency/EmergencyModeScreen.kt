package com.offgridrescue.app.ui.emergency

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgridrescue.app.ble.AppPermissions
import com.offgridrescue.app.ble.BleAdvertisingState
import com.offgridrescue.app.domain.AppMode
import com.offgridrescue.app.domain.EmergencyStatus
import com.offgridrescue.app.ui.theme.JeevanSetuTheme

@Composable
fun EmergencyModeRoute(
    viewModel: EmergencyViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.startEmergency()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.startEmergency()
    }

    LaunchedEffect(Unit) {
        viewModel.startEmergency()
    }

    EmergencyModeScreen(
        uiState = uiState,
        onRequestBluetoothPermission = {
            permissionLauncher.launch(AppPermissions.ADVERTISE)
        },
        onEnableBluetooth = {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        },
        onRetry = { viewModel.startEmergency() },
        onEndEmergency = {
            viewModel.endEmergency()
            onBackClick()
        },
        onBackHomeWithoutEnding = onBackClick,
        showEnableBluetooth = uiState.bleAdvertisingState == BleAdvertisingState.BLUETOOTH_OFF &&
            AppPermissions.areGranted(context, AppPermissions.ADVERTISE),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyModeScreen(
    uiState: EmergencyUiState,
    onRequestBluetoothPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRetry: () -> Unit,
    onEndEmergency: () -> Unit,
    onBackHomeWithoutEnding: () -> Unit,
    showEnableBluetooth: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Emergency Mode") })
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
            StatusCard(label = "Emergency status", value = uiState.emergencyStatus.displayName)
            StatusCard(label = "BLE advertising", value = uiState.bleAdvertisingState.displayName)
            StatusCard(
                label = "Anonymous device ID",
                value = uiState.anonymousIdHex ?: "None yet"
            )

            Text(
                text = "This ID is random. The advertise packet does not include your name, " +
                    "phone number, email, or GPS coordinates.",
                style = MaterialTheme.typography.bodyMedium
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            when (uiState.bleAdvertisingState) {
                BleAdvertisingState.PERMISSION_NEEDED -> {
                    Button(
                        onClick = onRequestBluetoothPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Bluetooth advertising")
                    }
                }
                BleAdvertisingState.BLUETOOTH_OFF -> {
                    if (showEnableBluetooth) {
                        Button(
                            onClick = onEnableBluetooth,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn on Bluetooth")
                        }
                    }
                }
                BleAdvertisingState.FAILED,
                BleAdvertisingState.NOT_SUPPORTED -> {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try again")
                    }
                }
                else -> Unit
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onEndEmergency,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Emergency Mode")
            }
            OutlinedButton(
                onClick = onBackHomeWithoutEnding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
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
private fun EmergencyModeScreenPreview() {
    JeevanSetuTheme {
        EmergencyModeScreen(
            uiState = EmergencyUiState(
                currentMode = AppMode.EMERGENCY,
                emergencyStatus = EmergencyStatus.ACTIVE,
                bleAdvertisingState = BleAdvertisingState.ADVERTISING,
                anonymousIdHex = "a1b2c3d4e5f60708"
            ),
            onRequestBluetoothPermission = {},
            onEnableBluetooth = {},
            onRetry = {},
            onEndEmergency = {},
            onBackHomeWithoutEnding = {},
            showEnableBluetooth = false
        )
    }
}
