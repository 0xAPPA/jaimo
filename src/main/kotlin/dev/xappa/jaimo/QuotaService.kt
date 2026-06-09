package dev.xappa.jaimo

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.Alarm
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.CopyOnWriteArrayList

fun interface QuotaStateListener {
    fun onStateChanged(state: QuotaState)
}

@Service(Service.Level.APP)
class QuotaService : Disposable {

    private val log = Logger.getInstance(QuotaService::class.java)
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val listeners = CopyOnWriteArrayList<QuotaStateListener>()

    @Volatile
    var state: QuotaState = QuotaState()
        private set

    @Volatile
    var previousQuota: Long? = null
        private set

    fun refresh() {
        ApplicationManager.getApplication().executeOnPooledThread {
            doRefresh()
        }
    }

    fun addListener(listener: QuotaStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: QuotaStateListener) {
        listeners.remove(listener)
    }

    private fun doRefresh() {
        try {
            val logFile = resolveLogFile()
            previousQuota = state.quota?.current
            state = QuotaLogParser.parse(logFile)
        } catch (e: Exception) {
            log.warn("Failed to parse quota from idea.log", e)
            state = QuotaState(error = e.message)
        }
        for (listener in listeners) {
            try {
                listener.onStateChanged(state)
            } catch (e: Exception) {
                log.warn("Listener error", e)
            }
        }
        scheduleNextRefresh()
    }

    private fun scheduleNextRefresh() {
        val timestamp = state.quota?.timestamp
        val delayMs = if (timestamp != null) {
            try {
                val dt = LocalDateTime.parse(timestamp, TIMESTAMP_PARSER)
                val nextRefreshAt = dt.plusSeconds((REFRESH_INTERVAL_MS + REFRESH_BUFFER_MS) / 1000)
                val delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), nextRefreshAt)
                if (delay > 0) delay else RETRY_DELAY_MS
            } catch (_: Exception) {
                REFRESH_INTERVAL_MS.toLong()
            }
        } else {
            RETRY_DELAY_MS
        }
        alarm.cancelAllRequests()
        alarm.addRequest({ doRefresh() }, delayMs)
    }

    private fun resolveLogFile(): Path {
        return Path.of(PathManager.getLogPath(), "idea.log")
    }

    override fun dispose() {}

    companion object {
        const val REFRESH_INTERVAL_MS = 10 * 60 * 1000 + 30_000
        const val REFRESH_BUFFER_MS = 30_000L
        private const val RETRY_DELAY_MS = 60_000L
        private val TIMESTAMP_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        @JvmStatic
        fun getInstance(): QuotaService =
            ApplicationManager.getApplication().getService(QuotaService::class.java)
    }
}
