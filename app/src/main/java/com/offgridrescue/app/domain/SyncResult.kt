package com.offgridrescue.app.domain

enum class SyncResult {
    SERVER_CONFIRMED,
    PENDING_OFFLINE,
    PERMISSION_DENIED,
    NETWORK_ERROR,
    AUTH_ERROR,
    UNKNOWN_ERROR
}
