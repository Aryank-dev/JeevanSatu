package com.offgridrescue.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.offgridrescue.app.device.BatteryReader
import com.offgridrescue.app.data.location.SimulatedAlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val batteryReader = BatteryReader(application.applicationContext)
    private val alertRepo = SimulatedAlertRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshBattery()
        
        alertRepo.getActiveAlerts()
            .onEach { alerts ->
                _uiState.update { it.copy(activeAlerts = alerts) }
            }
            .launchIn(viewModelScope)
    }

    fun refreshBattery() {
        _uiState.update { it.copy(batteryPercent = batteryReader.readPercent()) }
    }
}
