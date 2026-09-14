package com.offgridrescue.app.domain

enum class AppMode(val displayName: String) {
    STANDBY("Standby"),
    EMERGENCY("Emergency"),
    VICTIM("Victim"),
    RESCUER("Rescuer")
}
