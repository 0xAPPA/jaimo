package dev.xappa.jaimo

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class QuotaLogParserTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun logFile(vararg lines: String) =
        tmp.newFile("idea.log").toPath().also { it.toFile().writeText(lines.joinToString("\n")) }

    @Test
    fun `parses both quota and refill`() {
        val path = logFile(
            "2026-06-05 09:03:28,123 [  12345]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota state is: Available(current=6685635.02, maximum=25000000, until=xxx)",
            "2026-06-05 09:03:28,456 [  12345]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota refill state is: Known(next=2026-06-13T14:00:00.008Z, tariff=QuotaRefillInfoTariff(amount=25000000, duration=30d))",
        )
        val state = QuotaLogParser.parse(path)

        assertNull(state.error)
        assertNotNull(state.quota)
        assertEquals(6685635L, state.quota!!.current)
        assertEquals(25000000L, state.quota!!.maximum)
        assertEquals("2026-06-05 09:03:28", state.quota!!.timestamp)

        assertNotNull(state.refill)
        assertEquals(LocalDate.of(2026, 6, 13), state.refill!!.date)
    }

    @Test
    fun `quota only, no refill`() {
        val path = logFile(
            "2026-06-05 09:00:00,000 [  1]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota state is: Available(current=1000000, maximum=25000000, until=xxx)",
        )
        val state = QuotaLogParser.parse(path)

        assertNotNull(state.quota)
        assertEquals(1000000L, state.quota!!.current)
        assertNull(state.refill)
    }

    @Test
    fun `no matching lines`() {
        val path = logFile(
            "2026-06-05 09:00:00,000 some random log line",
            "2026-06-05 09:01:00,000 another line",
        )
        val state = QuotaLogParser.parse(path)

        assertNull(state.quota)
        assertNull(state.refill)
        assertNull(state.error)
    }

    @Test
    fun `empty file`() {
        val path = tmp.newFile("empty.log").toPath()
        val state = QuotaLogParser.parse(path)

        assertNull(state.quota)
        assertNull(state.refill)
    }

    @Test
    fun `multiple entries picks last`() {
        val path = logFile(
            "2026-06-05 08:00:00,000 [  1]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota state is: Available(current=20000000, maximum=25000000, until=xxx)",
            "2026-06-05 08:00:00,000 [  1]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota refill state is: Known(next=2026-06-10T14:00:00.008Z, tariff=xxx)",
            "2026-06-05 09:00:00,000 [  1]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota state is: Available(current=5000000, maximum=25000000, until=xxx)",
            "2026-06-05 09:00:00,000 [  1]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota refill state is: Known(next=2026-06-13T14:00:00.008Z, tariff=xxx)",
        )
        val state = QuotaLogParser.parse(path)

        assertEquals(5000000L, state.quota!!.current)
        assertEquals(LocalDate.of(2026, 6, 13), state.refill!!.date)
    }

    @Test
    fun `integer current value (no decimal)`() {
        val path = logFile(
            "2026-06-05 09:00:00,000 [  1]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota state is: Available(current=25000000, maximum=25000000, until=xxx)",
        )
        val state = QuotaLogParser.parse(path)

        assertEquals(25000000L, state.quota!!.current)
    }
}
