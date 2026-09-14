package com.offgridrescue.app.ui.emergency

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offgridrescue.app.ble.*
import com.offgridrescue.app.device.BatteryReader
import com.offgridrescue.app.device.LocationClient
import com.offgridrescue.app.device.NetworkMonitor
import com.offgridrescue.app.data.location.*
import com.offgridrescue.app.domain.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "EmergencyViewModel"
    private val advertiser = EmergencyBleAdvertiser(application.applicationContext)
    private val scanner = EmergencyBleScanner(application.applicationContext)
    private val locationClient = LocationClient(application.applicationContext)
    private val networkMonitor = NetworkMonitor(application.applicationContext)
    private val analyzer = ProximityAnalyzer()
    private val batteryReader = BatteryReader(application.applicationContext)
    
    private val locationDb = LocationTrackingDatabase.getDatabase(application.applicationContext)
    private val locationDao = locationDb.locationDao()
    private val checkInDao = locationDb.checkInDao()
    private val alertRepo = SimulatedAlertRepository()
    private val cloudRepo: CloudTrackingRepository = FirebaseTrackingRepository()

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private var sourceDeviceId: ByteArray? = null
    private var currentPacket: EmergencyPacket? = null
    private var cloudSessionId: String? = null
    
    private var staleCleanupJob: Job? = null
    private var locationTrackingJob: Job? = null
    private var cloudSyncJob: Job? = null
    private val STALE_TIMEOUT_SECONDS = 20L
    private val LOCATION_UPDATE_INTERVAL_MS = 30000L

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                Log.d(tag, "System Bluetooth state changed: $state")
                if (state == BluetoothAdapter.STATE_ON && _uiState.value.emergencyStatus == EmergencyStatus.ACTIVE) {
                    Log.i(tag, "Bluetooth turned back ON. Resuming emergency broadcast.")
                    startEmergency()
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.w(tag, "Bluetooth turned OFF. Updating UI state.")
                    _uiState.update { it.copy(bleAdvertisingState = BleAdvertisingState.BLUETOOTH_OFF) }
                    advertiser.stop()
                    scanner.stop()
                }
            }
        }
    }

    init {
        Log.d(tag, "Initializing EmergencyViewModel and registering Bluetooth state receiver.")
        application.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        // Step 12: Observe location availability for automatic resumption
        locationClient.observeLocationEnabled()
            .onEach { enabled ->
                handleLocationAvailabilityChanged(enabled)
            }
            .launchIn(viewModelScope)

        // Step 14: Observe simulated alerts
        alertRepo.getActiveAlerts()
            .onEach { alerts ->
                _uiState.update { it.copy(activeAlerts = alerts) }
            }
            .launchIn(viewModelScope)

        // Step 15 Refinement: Authoritative Network Monitoring
        networkMonitor.status
            .onEach { status ->
                Log.d(tag, "[NETWORK] Status: $status")
                _uiState.update { it.copy(
                    networkStatus = status,
                    cloudConnectionStatus = if (status == NetworkStatus.ONLINE) 
                        CloudConnectionStatus.CONNECTED else CloudConnectionStatus.DISCONNECTED
                ) }
                if (status == NetworkStatus.ONLINE && _uiState.value.emergencyStatus == EmergencyStatus.ACTIVE) {
                    val sessionId = cloudSessionId
                    if (sessionId != null) {
                        Log.i(tag, "[NETWORK] Restored. Triggering automatic sync retry for session: $sessionId")
                        triggerSyncRetry(sessionId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleLocationAvailabilityChanged(enabled: Boolean) {
        Log.d(tag, "[GPS] Availability changed: $enabled")
        val status = if (enabled) {
            if (AppPermissions.areGranted(getApplication(), AppPermissions.LOCATION)) GpsStatus.ACTIVE
            else GpsStatus.PERMISSION_DENIED
        } else GpsStatus.UNAVAILABLE
        
        _uiState.update { it.copy(gpsStatus = status) }

        if (enabled) {
            val sessionId = cloudSessionId
            if (_uiState.value.emergencyStatus == EmergencyStatus.ACTIVE &&
                sessionId != null &&
                status == GpsStatus.ACTIVE
            ) {
                if (locationTrackingJob == null) {
                    Log.i(tag, "[GPS-LIFECYCLE] Tracking resumed automatically. Session: $sessionId")
                    startLocationTracking(sessionId)
                } else {
                    Log.d(tag, "[GPS-LIFECYCLE] Tracking already active. Session: $sessionId")
                }
            }
        } else {
            if (locationTrackingJob != null) {
                Log.w(tag, "[GPS-LIFECYCLE] Tracking stopped (GPS disabled by user). Session: $cloudSessionId")
                locationTrackingJob?.cancel()
                locationTrackingJob = null
            }
        }
    }

    private fun triggerSyncRetry(sessionId: String) {
        viewModelScope.launch {
            // 1. Retry Locations (Isolated)
            val pendingLoc = locationDao.getPendingRecords(sessionId)
            if (pendingLoc.isNotEmpty()) {
                Log.i(tag, "[LOCATION-SYNC] Network restored. Retrying ${pendingLoc.size} records.")
                pendingLoc.forEach { syncRecordToCloud(sessionId, it) }
            }

            // 2. Retry Check-ins (Isolated)
            val pendingCheck = checkInDao.getPendingRecords(sessionId)
            if (pendingCheck.isNotEmpty()) {
                Log.i(tag, "[CHECKIN-SYNC] Network restored. Retrying ${pendingCheck.size} records.")
                pendingCheck.forEach { syncCheckInToCloud(sessionId, it) }
            }
        }
    }

    fun onSosTypeSelected(sosType: SosType) {
        _uiState.update { it.copy(selectedSosType = sosType) }
    }

    fun onCheckInSelected(status: CheckInStatus) {
        val sessionId = cloudSessionId ?: return
        viewModelScope.launch {
            val recordId = UUID.randomUUID().toString()
            val record = CheckInRecordEntity(
                id = recordId,
                cloudSessionId = sessionId,
                status = status,
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
            checkInDao.insert(record)
            Log.d(tag, "[CHECKIN-LOCAL] Saved to Room: $recordId")
            _uiState.update { it.copy(currentCheckInStatus = status) }
            updateSyncStatus()
            
            // Immediate sync attempt (Isolated)
            if (_uiState.value.networkStatus == NetworkStatus.ONLINE) {
                viewModelScope.launch {
                    syncCheckInToCloud(sessionId, record)
                }
            } else {
                Log.d(tag, "[CHECKIN-SYNC] Network offline. Record ${record.id} will remain PENDING.")
            }
        }
    }

    private suspend fun syncCheckInToCloud(sessionId: String, record: CheckInRecordEntity) {
        val auth = FirebaseAuth.getInstance()
        val ownerUid = auth.currentUser?.uid ?: run {
            Log.e(tag, "[CHECKIN-SYNC] ERROR: No Auth UID")
            _uiState.update { it.copy(checkInSyncStatus = CloudSyncStatus.AUTH_ERROR) }
            return
        }
        
        Log.d(tag, "[CHECKIN-SYNC] QUEUED: ${record.id}. Auth UID: $ownerUid")
        try {
            val result = cloudRepo.syncCheckIn(sessionId, record, ownerUid)
            when (result) {
                SyncResult.SERVER_CONFIRMED -> {
                    checkInDao.markAsSynced(record.id)
                    Log.i(tag, "[CHECKIN-SYNC] SERVER_CONFIRMED: ${record.id}")
                    _uiState.update { it.copy(checkInSyncError = null) }
                }
                SyncResult.PENDING_OFFLINE -> {
                    Log.d(tag, "[CHECKIN-SYNC] PENDING_OFFLINE: ${record.id}")
                }
                SyncResult.PERMISSION_DENIED -> {
                    Log.e(tag, "[CHECKIN-SYNC] ERROR (PERMISSION_DENIED): ${record.id}")
                    _uiState.update { it.copy(checkInSyncError = "Permission Denied") }
                }
                else -> {
                    Log.w(tag, "[CHECKIN-SYNC] ERROR (UNKNOWN): ${record.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "[CHECKIN-SYNC] ERROR: ${record.id}", e)
        } finally {
            updateSyncStatus()
        }
    }

    fun startEmergency() {
        Log.d(tag, "startEmergency() called.")
        val context = getApplication<Application>().applicationContext

        if (!advertiser.hasBleHardware()) {
            Log.e(tag, "Emergency start failed: BLE hardware missing.")
            _uiState.update {
                it.copy(
                    currentMode = AppMode.EMERGENCY,
                    emergencyStatus = EmergencyStatus.ACTIVE,
                    bleAdvertisingState = BleAdvertisingState.NOT_SUPPORTED,
                    errorMessage = "This device does not support BLE advertising."
                )
            }
            return
        }

        if (!AppPermissions.areGranted(context, AppPermissions.ADVERTISE)) {
            Log.w(tag, "Emergency start delayed: Permissions needed.")
            _uiState.update {
                it.copy(
                    currentMode = AppMode.EMERGENCY,
                    emergencyStatus = EmergencyStatus.ACTIVE,
                    bleAdvertisingState = BleAdvertisingState.PERMISSION_NEEDED,
                    errorMessage = "Allow Bluetooth advertising to broadcast an anonymous ID."
                )
            }
            return
        }

        if (!advertiser.isBluetoothEnabled()) {
            Log.w(tag, "Emergency start delayed: Bluetooth is OFF.")
            _uiState.update {
                it.copy(
                    currentMode = AppMode.EMERGENCY,
                    emergencyStatus = EmergencyStatus.ACTIVE,
                    bleAdvertisingState = BleAdvertisingState.BLUETOOTH_OFF,
                    errorMessage = "Turn on Bluetooth to advertise."
                )
            }
            return
        }

        if (_uiState.value.bleAdvertisingState == BleAdvertisingState.ADVERTISING ||
            _uiState.value.bleAdvertisingState == BleAdvertisingState.STARTING
        ) {
            Log.d(tag, "Already advertising or starting. Ignoring request.")
            return
        }

        // 1. Get sourceDeviceId (stable for the emergency)
        val sId = sourceDeviceId ?: AnonymousDeviceId.generate().also { sourceDeviceId = it }
        
        // 2. Capture battery snapshot
        val battery = batteryReader.readPercent()
        
        // 3. Create stable EmergencyPacket
        val packet = currentPacket ?: EmergencyPacket(
            packetId = ByteArray(4).apply { SecureRandom().nextBytes(this) },
            sourceDeviceId = sId,
            sosType = _uiState.value.selectedSosType,
            batteryLevel = battery,
            createdAt = Instant.now(),
            hopCount = 0,
            ttlMinutes = 60
        ).also { 
            currentPacket = it
            Log.i(tag, "[BLE-TX] Emergency packet created")
            Log.i(tag, "[BLE-TX] Packet ID: ${it.toHexPacketId()}")
            Log.i(tag, "[BLE-TX] Source Device ID: ${it.toHexSourceId()}")
        }
        
        _uiState.update {
            it.copy(
                currentMode = AppMode.EMERGENCY,
                emergencyStatus = EmergencyStatus.ACTIVE,
                bleAdvertisingState = BleAdvertisingState.STARTING,
                anonymousIdHex = packet.toHexSourceId(),
                batteryLevel = battery,
                errorMessage = null
            )
        }

        advertiser.start(
            packet = packet,
            listener = object : EmergencyBleAdvertiser.Listener {
                override fun onStarted() {
                    Log.i(tag, "Emergency BLE broadcast listener: onStarted()")
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        _uiState.update { state ->
                            state.copy(
                                bleAdvertisingState = BleAdvertisingState.ADVERTISING,
                                errorMessage = null
                            )
                        }
                    }
                }

                override fun onFailed(message: String) {
                    Log.e(tag, "Emergency BLE broadcast listener: onFailed($message)")
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        _uiState.update { state ->
                            state.copy(
                                bleAdvertisingState = BleAdvertisingState.FAILED,
                                errorMessage = message
                            )
                        }
                    }
                }
            }
        )

        // Start nearby awareness scanning
        startNearbyAwareness()

        // Start Cloud Tracking
        startCloudTracking(packet)
    }

    private fun startCloudTracking(packet: EmergencyPacket) {
        viewModelScope.launch {
            try {
                Log.d(tag, "[CLOUD] Starting setup...")
                _uiState.update { it.copy(cloudConnectionStatus = CloudConnectionStatus.UNKNOWN) }
                // Anonymous Auth
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }
                val ownerUid = auth.currentUser?.uid ?: throw Exception("Firebase Auth failed")

                // Session initialization
                val sessionId = UUID.randomUUID().toString()
                cloudSessionId = sessionId
                
                val session = CloudSession(
                    cloudSessionId = sessionId,
                    ownerUid = ownerUid,
                    sourceDeviceId = packet.toHexSourceId(),
                    blePacketId = packet.toHexPacketId(),
                    sosType = packet.sosType.displayName,
                    startedAt = Timestamp.now()
                )
                
                val result = cloudRepo.createSession(session)
                if (result == SyncResult.SERVER_CONFIRMED) {
                    Log.i(tag, "[CLOUD] Session created: $sessionId. Auth UID: $ownerUid")
                    _uiState.update { it.copy(
                        locationSyncStatus = CloudSyncStatus.IDLE, 
                        checkInSyncStatus = CloudSyncStatus.IDLE,
                        cloudConnectionStatus = CloudConnectionStatus.CONNECTED
                    ) }
                    
                    // Start GPS if permission granted
                    if (AppPermissions.areGranted(getApplication(), AppPermissions.LOCATION)) {
                        startLocationTracking(sessionId)
                    } else {
                        Log.w(tag, "[GPS-LIFECYCLE] Location permission missing. GPS tracking disabled.")
                        _uiState.update { it.copy(gpsStatus = GpsStatus.PERMISSION_DENIED) }
                    }
                } else {
                    Log.e(tag, "[CLOUD] Failed to create Firestore session. Status: $result")
                    _uiState.update { it.copy(cloudConnectionStatus = CloudConnectionStatus.DISCONNECTED) }
                }
            } catch (e: Exception) {
                Log.e(tag, "[CLOUD] Setup failed", e)
                _uiState.update { it.copy(cloudConnectionStatus = CloudConnectionStatus.DISCONNECTED) }
            }
        }
    }

    private fun startLocationTracking(sessionId: String) {
        if (locationTrackingJob != null) {
            Log.d(tag, "[GPS-LIFECYCLE] startLocationTracking called but already active. Session: $sessionId")
            return
        }

        Log.i(tag, "[GPS-LIFECYCLE] Tracking started. Session: $sessionId")
        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(tag, "[GPS-LIFECYCLE] Auth UID: $authUid")

        _uiState.update { it.copy(gpsStatus = GpsStatus.ACTIVE) }

        locationTrackingJob = locationClient.getLocationUpdates(LOCATION_UPDATE_INTERVAL_MS)
            .onEach { location ->
                Log.d(tag, "[GPS-RX] Location received: ${location.latitude}, ${location.longitude}, acc: ${location.accuracy}")
                saveLocationLocally(sessionId, location)
            }
            .launchIn(viewModelScope)
            
        // Start background sync loop for pending records
        if (cloudSyncJob == null) {
            startCloudSyncLoop(sessionId)
        }
    }

    private suspend fun saveLocationLocally(sessionId: String, location: android.location.Location) {
        val recordId = UUID.randomUUID().toString()
        val record = LocationRecordEntity(
            id = recordId,
            cloudSessionId = sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        locationDao.insert(record)
        Log.d(tag, "[LOCATION-LOCAL] Saved to Room: $recordId")
        
        _uiState.update { it.copy(lastKnownLocation = location) }
        updateSyncStatus()
        
        // Immediate sync attempt (Isolated)
        if (_uiState.value.networkStatus == NetworkStatus.ONLINE) {
            viewModelScope.launch {
                syncRecordToCloud(sessionId, record)
            }
        } else {
            Log.d(tag, "[LOCATION-SYNC] Network offline. Record ${record.id} will remain PENDING.")
        }
    }

    private suspend fun syncRecordToCloud(sessionId: String, record: LocationRecordEntity) {
        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(tag, "[LOCATION-SYNC] QUEUED: ${record.id}. Session: $sessionId. Auth UID: $authUid")
        
        try {
            val result = cloudRepo.syncLocation(sessionId, record)
            when (result) {
                SyncResult.SERVER_CONFIRMED -> {
                    locationDao.markAsSynced(record.id)
                    Log.i(tag, "[LOCATION-SYNC] SERVER_CONFIRMED: ${record.id}")
                    _uiState.update { it.copy(locationSyncError = null) }
                }
                SyncResult.PENDING_OFFLINE -> {
                    Log.d(tag, "[LOCATION-SYNC] PENDING_OFFLINE: ${record.id}")
                }
                SyncResult.PERMISSION_DENIED -> {
                    Log.e(tag, "[LOCATION-SYNC] ERROR (PERMISSION_DENIED): ${record.id}")
                    _uiState.update { it.copy(locationSyncError = "Permission Denied") }
                }
                else -> {
                    Log.w(tag, "[LOCATION-SYNC] ERROR (UNKNOWN): ${record.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "[LOCATION-SYNC] ERROR: ${record.id}", e)
        } finally {
            updateSyncStatus()
        }
    }

    private fun startCloudSyncLoop(sessionId: String) {
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            while (true) {
                delay(60000) // Retry every minute
                Log.d(tag, "[CLOUD-SYNC] Starting background retry loop for session: $sessionId")
                
                if (_uiState.value.networkStatus == NetworkStatus.ONLINE) {
                    // 1. Retry Locations (Isolated)
                    launch {
                        val pendingLocations = locationDao.getPendingRecords(sessionId)
                        if (pendingLocations.isNotEmpty()) {
                            Log.i(tag, "[LOCATION-SYNC] Retrying ${pendingLocations.size} pending records")
                            pendingLocations.forEach { syncRecordToCloud(sessionId, it) }
                        }
                    }

                    // 2. Retry Check-ins (Isolated)
                    launch {
                        val pendingCheckIns = checkInDao.getPendingRecords(sessionId)
                        if (pendingCheckIns.isNotEmpty()) {
                            Log.i(tag, "[CHECKIN-SYNC] Retrying ${pendingCheckIns.size} pending records")
                            pendingCheckIns.forEach { syncCheckInToCloud(sessionId, it) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateSyncStatus() {
        val sessionId = cloudSessionId ?: return
        val pendingLoc = locationDao.getPendingRecords(sessionId)
        val pendingCheck = checkInDao.getPendingRecords(sessionId)
        
        _uiState.update { state ->
            state.copy(
                locationSyncStatus = when {
                    state.locationSyncError != null -> CloudSyncStatus.ERROR
                    pendingLoc.isEmpty() -> CloudSyncStatus.SYNCED
                    state.networkStatus == NetworkStatus.OFFLINE -> CloudSyncStatus.PENDING
                    else -> CloudSyncStatus.SYNCING
                },
                checkInSyncStatus = when {
                    state.checkInSyncError != null -> CloudSyncStatus.ERROR
                    pendingCheck.isEmpty() -> CloudSyncStatus.SYNCED
                    state.networkStatus == NetworkStatus.OFFLINE -> CloudSyncStatus.PENDING
                    else -> CloudSyncStatus.SYNCING
                }
            )
        }
    }

    private fun classifyException(e: Throwable?): CloudSyncStatus {
        if (e == null) return CloudSyncStatus.IDLE
        return if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            CloudSyncStatus.ERROR
        } else {
            CloudSyncStatus.PENDING
        }
    }

    private fun startNearbyAwareness() {
        Log.d(tag, "Attempting to start nearby awareness scan.")
        
        // Priority check for Scan permissions
        if (!AppPermissions.areGranted(getApplication<Application>().applicationContext, AppPermissions.SCAN)) {
            Log.w(tag, "Nearby awareness start failed: Scan permissions not granted.")
            _uiState.update { it.copy(nearbyAwarenessError = "Scan permission required for mesh awareness.") }
            return
        }

        scanner.start(object : EmergencyBleScanner.Listener {
            override fun onPacketDetected(packet: EmergencyPacket, rssi: Int) {
                processNearbyPacket(packet, rssi)
            }

            override fun onError(message: String) {
                Log.e(tag, "Nearby awareness scanner error: $message")
                _uiState.update { it.copy(nearbyAwarenessError = "Mesh awareness unavailable: $message") }
            }
        })

        startStaleCleanupJob()
    }

    private fun processNearbyPacket(packet: EmergencyPacket, rssi: Int) {
        val currentSourceId = sourceDeviceId ?: return
        
        // Self-Filter
        if (packet.sourceDeviceId.contentEquals(currentSourceId)) {
            return
        }

        // Validate
        if (!PacketValidator.isValid(packet)) {
            return
        }

        val deviceId = packet.toHexSourceId()
        
        _uiState.update { state ->
            val existing = state.nearbyDevices.find { it.deviceId == deviceId }
            val updatedDevice = analyzer.analyze(deviceId, rssi, existing)
                .copy(latestPacket = packet)
            
            val newDevices = state.nearbyDevices.toMutableList()
            val index = newDevices.indexOfFirst { it.deviceId == deviceId }
            if (index != -1) {
                newDevices[index] = updatedDevice
            } else {
                newDevices.add(updatedDevice)
            }
            
            state.copy(
                nearbyDevices = newDevices.sortedBy { it.proximity.ordinal },
                nearbyAwarenessError = null
            )
        }
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
            val activeDevices = state.nearbyDevices.filter {
                Duration.between(it.lastSeen, now).seconds < STALE_TIMEOUT_SECONDS
            }
            if (activeDevices.size != state.nearbyDevices.size) {
                Log.d(tag, "Pruned ${state.nearbyDevices.size - activeDevices.size} stale devices.")
                state.copy(nearbyDevices = activeDevices)
            } else {
                state
            }
        }
    }

    fun endEmergency() {
        Log.i(tag, "endEmergency() called. Stopping advertiser, scanner, and tracking.")
        advertiser.stop()
        scanner.stop()
        staleCleanupJob?.cancel()
        staleCleanupJob = null
        locationTrackingJob?.cancel()
        locationTrackingJob = null
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        sourceDeviceId = null
        currentPacket = null
        cloudSessionId = null
        _uiState.update { EmergencyUiState() }
    }

    override fun onCleared() {
        Log.d(tag, "onCleared() called. Cleaning up resources.")
        try {
            getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering receiver: ${e.message}")
        }
        advertiser.stop()
        scanner.stop()
        staleCleanupJob?.cancel()
        locationTrackingJob?.cancel()
        cloudSyncJob?.cancel()
        super.onCleared()
    }
}
