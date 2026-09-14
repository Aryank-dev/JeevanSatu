package com.offgridrescue.app.ui.home

import com.offgridrescue.app.domain.AppMode
import com.offgridrescue.app.domain.EmergencyStatus
import com.offgridrescue.app.domain.OfficialAlert

data class HomeUiState(
    val appName: String = "JeevanSetu",
    val currentMode: AppMode = AppMode.STANDBY,
    val emergencyStatus: EmergencyStatus = EmergencyStatus.INACTIVE,
    val batteryPercent: Int = 0,
    val bleStatus: String = "Not advertising",
    val activeAlerts: List<OfficialAlert> = emptyList()
)
