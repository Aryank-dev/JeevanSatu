package com.offgridrescue.app.ui.rescuer

import com.offgridrescue.app.domain.UnifiedIncident
import com.offgridrescue.app.domain.HeatmapNode
import com.offgridrescue.app.ui.emergency.CloudConnectionStatus

data class ResponderUiState(
    val incidents: List<UnifiedIncident> = emptyList(),
    val heatmapNodes: List<HeatmapNode> = emptyList(),
    val isAuthorized: Boolean = false,
    val isOnline: Boolean = true,
    val cloudConnectionStatus: CloudConnectionStatus = CloudConnectionStatus.UNKNOWN,
    val isScanning: Boolean = false,
    val error: String? = null
)
