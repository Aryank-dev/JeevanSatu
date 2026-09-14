package com.offgridrescue.app.domain

import com.google.firebase.Timestamp

data class CloudSession(
    val cloudSessionId: String,
    val ownerUid: String,
    val sourceDeviceId: String,
    val blePacketId: String,
    val sosType: String,
    val startedAt: Timestamp
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "ownerUid" to ownerUid,
            "sourceDeviceId" to sourceDeviceId,
            "blePacketId" to blePacketId,
            "sosType" to sosType,
            "startedAt" to startedAt
        )
    }
}
