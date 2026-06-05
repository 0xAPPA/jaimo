package dev.xappa.jaimo

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.LocalDate

object QuotaLogParser {

    private val QUOTA_PATTERN = Regex("""current=(\d+(?:\.\d+)?),\s*maximum=(\d+)""")
    private val REFILL_PATTERN = Regex("""next=(\d{4}-\d{2}-\d{2})T""")
    private val TIMESTAMP_PATTERN = Regex("""^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})""")

    private const val QUOTA_MARKER = "QuotaManager2Impl - New quota state is: Available"
    private const val REFILL_MARKER = "QuotaManager2Impl - New quota refill state is: Known"

    fun parse(logFile: Path): QuotaState {
        try {
            var quotaInfo: QuotaInfo? = null
            var refillInfo: RefillInfo? = null

            for (line in readLinesReversed(logFile)) {
                if (quotaInfo == null && line.contains(QUOTA_MARKER)) {
                    quotaInfo = parseQuotaLine(line)
                }
                if (refillInfo == null && line.contains(REFILL_MARKER)) {
                    refillInfo = parseRefillLine(line)
                }
                if (quotaInfo != null && refillInfo != null) break
            }

            return QuotaState(quota = quotaInfo, refill = refillInfo)
        } catch (e: Exception) {
            return QuotaState(error = e.message ?: "Unknown error")
        }
    }

    private fun parseQuotaLine(line: String): QuotaInfo? {
        val match = QUOTA_PATTERN.find(line) ?: return null
        val current = match.groupValues[1].toDoubleOrNull()?.toLong() ?: return null
        val maximum = match.groupValues[2].toLongOrNull() ?: return null
        val timestamp = TIMESTAMP_PATTERN.find(line)?.groupValues?.get(1) ?: ""
        return QuotaInfo(current = current, maximum = maximum, timestamp = timestamp)
    }

    private fun parseRefillLine(line: String): RefillInfo? {
        val match = REFILL_PATTERN.find(line) ?: return null
        val date = try {
            LocalDate.parse(match.groupValues[1])
        } catch (_: Exception) {
            return null
        }
        return RefillInfo(date = date)
    }

    private fun readLinesReversed(path: Path): Sequence<String> = sequence {
        val file = path.toFile()
        if (!file.exists() || file.length() == 0L) return@sequence

        RandomAccessFile(file, "r").use { raf ->
            var pos = raf.length() - 1
            val buffer = StringBuilder()

            while (pos >= 0) {
                raf.seek(pos)
                val ch = raf.read()
                if (ch == '\n'.code) {
                    if (buffer.isNotEmpty()) {
                        yield(buffer.reverse().toString())
                        buffer.clear()
                    }
                } else {
                    buffer.append(ch.toChar())
                }
                pos--
            }
            if (buffer.isNotEmpty()) {
                yield(buffer.reverse().toString())
            }
        }
    }
}
