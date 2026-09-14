package com.offgridrescue.app.domain

enum class CheckInStatus(val displayName: String) {
    SAFE("Safe"),
    NEEDS_HELP("Needs Help"),
    URGENT("Urgent")
}
