package com.offgridrescue.app.data.location

import com.offgridrescue.app.domain.AlertSeverity
import com.offgridrescue.app.domain.OfficialAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

interface OfficialAlertRepository {
    fun getActiveAlerts(): Flow<List<OfficialAlert>>
}

class SimulatedAlertRepository : OfficialAlertRepository {
    override fun getActiveAlerts(): Flow<List<OfficialAlert>> {
        val alerts = listOf(
            OfficialAlert(
                id = "demo_1",
                title = "Weather Warning",
                message = "Simulated heavy rainfall alert for the coastal region. Exercise caution.",
                severity = AlertSeverity.WARNING,
                timestamp = Instant.now()
            ),
            OfficialAlert(
                id = "demo_2",
                title = "Emergency Drill",
                message = "This is a simulated emergency drill alert. No action required.",
                severity = AlertSeverity.INFO,
                timestamp = Instant.now().minusSeconds(3600)
            )
        )
        return flowOf(alerts)
    }
}
