package com.offgridrescue.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityAnalyzerTest {
    private val analyzer = ProximityAnalyzer(windowSize = 5, smoothingAlpha = 1.0) // No smoothing for basic tests

    @Test
    fun `test proximity classification VERY CLOSE`() {
        val result = analyzer.analyze("device1", -50, null)
        assertEquals(ProximityLevel.VERY_CLOSE, result.proximity)
    }

    @Test
    fun `test proximity classification NEARBY`() {
        val result = analyzer.analyze("device1", -60, null)
        assertEquals(ProximityLevel.NEARBY, result.proximity)
    }

    @Test
    fun `test proximity classification FAR`() {
        val result = analyzer.analyze("device1", -75, null)
        assertEquals(ProximityLevel.FAR, result.proximity)
    }

    @Test
    fun `test signal trend STRONGER`() {
        val first = analyzer.analyze("device1", -70, null)
        val second = analyzer.analyze("device1", -60, first)
        assertEquals(SignalTrend.STRONGER, second.signalTrend)
    }

    @Test
    fun `test signal trend WEAKER`() {
        val first = analyzer.analyze("device1", -60, null)
        val second = analyzer.analyze("device1", -70, first)
        assertEquals(SignalTrend.WEAKER, second.signalTrend)
    }

    @Test
    fun `test signal trend STABLE`() {
        val first = analyzer.analyze("device1", -60, null)
        val second = analyzer.analyze("device1", -60, first)
        assertEquals(SignalTrend.STABLE, second.signalTrend)
    }

    @Test
    fun `test smoothing with alpha 0 point 5`() {
        val smoothedAnalyzer = ProximityAnalyzer(smoothingAlpha = 0.5)
        val first = smoothedAnalyzer.analyze("device1", -80, null)
        assertEquals(-80.0, first.smoothedRssi, 0.1)
        
        val second = smoothedAnalyzer.analyze("device1", -60, first)
        // 0.5 * (-60) + 0.5 * (-80) = -70
        assertEquals(-70.0, second.smoothedRssi, 0.1)
    }

    @Test
    fun `test multiple devices have independent histories`() {
        val firstA = analyzer.analyze("deviceA", -80, null)
        val firstB = analyzer.analyze("deviceB", -50, null)
        
        val secondA = analyzer.analyze("deviceA", -75, firstA)
        val secondB = analyzer.analyze("deviceB", -55, firstB)
        
        assertEquals(SignalTrend.STRONGER, secondA.signalTrend)
        assertEquals(SignalTrend.WEAKER, secondB.signalTrend)
    }
}
