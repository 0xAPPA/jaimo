package dev.xappa.jaimo

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.Alarm
import java.nio.file.Path
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

    init {
        scheduleRefresh()
    }

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

    private fun scheduleRefresh() {
        alarm.addRequest({
            doRefresh()
            scheduleRefresh()
        }, REFRESH_INTERVAL_MS)
    }

    private fun doRefresh() {
        try {
            val logFile = resolveLogFile()
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
    }

    private fun resolveLogFile(): Path {
        return Path.of(PathManager.getLogPath(), "idea.log")
    }

    override fun dispose() {}

    companion object {
        private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000

        @JvmStatic
        fun getInstance(): QuotaService =
            ApplicationManager.getApplication().getService(QuotaService::class.java)
    }
}
