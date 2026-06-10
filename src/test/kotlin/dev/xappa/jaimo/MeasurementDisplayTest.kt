package dev.xappa.jaimo

import org.junit.Assert.*
import org.junit.Test

class MeasurementDisplayTest {

    @Test
    fun `no measurement returns null`() {
        assertNull(QuotaPopup.buildMeasurementLine(MeasurementState()))
    }

    @Test
    fun `start only shows arrow marker`() {
        val m = MeasurementState(startTokens = 18_143_812, startTimestamp = "2026-06-09 09:03:28")
        val line = QuotaPopup.buildMeasurementLine(m)
        assertEquals("Measure:    \u25B6 18,143,812 (09:03)", line)
    }

    @Test
    fun `start and end shows delta`() {
        val m = MeasurementState(
            startTokens = 18_143_812, startTimestamp = "2026-06-09 09:03:28",
            endTokens = 18_456_212, endTimestamp = "2026-06-09 11:47:15",
        )
        val line = QuotaPopup.buildMeasurementLine(m)
        assertEquals("Measure:    18,143,812 \u2192 18,456,212 = +312,400 (09:03 \u2192 11:47)", line)
    }

    @Test
    fun `measurement line appears in full popup text`() {
        val state = QuotaState(
            quota = QuotaInfo(18_143_812, 25_000_000, "2026-06-09 09:03:28"),
        )
        val measurement = MeasurementState(startTokens = 18_000_000, startTimestamp = "2026-06-09 08:00:00")
        val text = QuotaPopup.buildText(state, null, measurement)
        assertTrue(text.contains("Measure:"))
        assertTrue(text.contains("\u25B6 18,000,000 (08:00)"))
    }
}
