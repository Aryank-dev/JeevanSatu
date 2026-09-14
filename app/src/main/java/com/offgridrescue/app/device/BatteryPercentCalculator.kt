package com.offgridrescue.app.device

object BatteryPercentCalculator {
    fun fromCapacityProperty(capacity: Int): Int? =
        capacity.takeIf { it in 0..100 }

    fun fromLevelAndScale(level: Int, scale: Int): Int {
        if (level < 0 || scale <= 0) return 0
        return ((level / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }
}
