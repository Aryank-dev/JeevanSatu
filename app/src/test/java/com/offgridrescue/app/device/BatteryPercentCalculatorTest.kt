package com.offgridrescue.app.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryPercentCalculatorTest {
    @Test
    fun fromCapacityProperty_acceptsValidRange() {
        assertEquals(0, BatteryPercentCalculator.fromCapacityProperty(0))
        assertEquals(100, BatteryPercentCalculator.fromCapacityProperty(100))
        assertEquals(42, BatteryPercentCalculator.fromCapacityProperty(42))
    }

    @Test
    fun fromCapacityProperty_rejectsInvalidValues() {
        assertNull(BatteryPercentCalculator.fromCapacityProperty(-1))
        assertNull(BatteryPercentCalculator.fromCapacityProperty(101))
        assertNull(BatteryPercentCalculator.fromCapacityProperty(Int.MIN_VALUE))
    }

    @Test
    fun fromLevelAndScale_computesPercent() {
        assertEquals(50, BatteryPercentCalculator.fromLevelAndScale(50, 100))
        assertEquals(0, BatteryPercentCalculator.fromLevelAndScale(-1, 100))
        assertEquals(0, BatteryPercentCalculator.fromLevelAndScale(10, 0))
    }
}
