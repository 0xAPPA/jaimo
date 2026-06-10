package dev.xappa.jaimo

import org.junit.Assert.*
import org.junit.Test

class MeasurementServiceTest {

    private fun createService() = MeasurementService()

    @Test
    fun `initial state is empty`() {
        val service = createService()
        val m = service.measurement
        assertNull(m.startTokens)
        assertNull(m.startTimestamp)
        assertNull(m.endTokens)
        assertNull(m.endTimestamp)
    }

    @Test
    fun `setStart stores tokens and timestamp`() {
        val service = createService()
        service.setStart(18_143_812, "2026-06-09 09:03:28")
        val m = service.measurement
        assertEquals(18_143_812L, m.startTokens)
        assertEquals("2026-06-09 09:03:28", m.startTimestamp)
        assertNull(m.endTokens)
        assertNull(m.endTimestamp)
    }

    @Test
    fun `setEnd stores end tokens and timestamp`() {
        val service = createService()
        service.setStart(18_143_812, "2026-06-09 09:03:28")
        service.setEnd(18_456_212, "2026-06-09 11:47:15")
        val m = service.measurement
        assertEquals(18_143_812L, m.startTokens)
        assertEquals(18_456_212L, m.endTokens)
        assertEquals("2026-06-09 11:47:15", m.endTimestamp)
    }

    @Test
    fun `clear resets all fields`() {
        val service = createService()
        service.setStart(18_143_812, "2026-06-09 09:03:28")
        service.setEnd(18_456_212, "2026-06-09 11:47:15")
        service.clear()
        val m = service.measurement
        assertNull(m.startTokens)
        assertNull(m.startTimestamp)
        assertNull(m.endTokens)
        assertNull(m.endTimestamp)
    }

    @Test
    fun `full cycle start - end - clear - start`() {
        val service = createService()
        service.setStart(1000, "2026-06-09 09:00:00")
        service.setEnd(2000, "2026-06-09 10:00:00")
        service.clear()
        service.setStart(3000, "2026-06-09 11:00:00")
        val m = service.measurement
        assertEquals(3000L, m.startTokens)
        assertEquals("2026-06-09 11:00:00", m.startTimestamp)
        assertNull(m.endTokens)
    }

    @Test
    fun `persistence round-trip via getState and loadState`() {
        val service = createService()
        service.setStart(18_143_812, "2026-06-09 09:03:28")
        service.setEnd(18_456_212, "2026-06-09 11:47:15")
        val saved = service.state

        val restored = createService()
        restored.loadState(saved)
        val m = restored.measurement
        assertEquals(18_143_812L, m.startTokens)
        assertEquals("2026-06-09 09:03:28", m.startTimestamp)
        assertEquals(18_456_212L, m.endTokens)
        assertEquals("2026-06-09 11:47:15", m.endTimestamp)
    }
}
