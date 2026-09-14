package com.offgridrescue.app.domain

enum class SosType(val id: Int, val displayName: String) {
    GENERAL(0, "General"),
    MEDICAL(1, "Medical"),
    FIRE(2, "Fire"),
    FLOOD(3, "Flood"),
    TRAPPED(4, "Trapped"),
    OTHER(5, "Other");

    companion object {
        fun fromId(id: Int): SosType = values().find { it.id == id } ?: GENERAL
    }
}
