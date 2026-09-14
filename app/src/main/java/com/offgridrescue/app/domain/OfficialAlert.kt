package com.offgridrescue.app.domain

import java.time.Instant

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class OfficialAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Instant,
    val agency: String = "SIMULATED AGENCY"
) {
    val displayTitle: String get() = "[SIMULATED] $title"
}
