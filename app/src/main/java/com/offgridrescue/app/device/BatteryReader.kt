package com.offgridrescue.app.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryReader(private val context: Context) {
    fun readPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val fromProperty = BatteryPercentCalculator.fromCapacityProperty(
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        )
        if (fromProperty != null) return fromProperty

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return BatteryPercentCalculator.fromLevelAndScale(level, scale)
    }
}
