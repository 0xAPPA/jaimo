package dev.xappa.jaimo

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "JaimoMeasurement", storages = [Storage("jaimo-measurement.xml")])
class MeasurementService : PersistentStateComponent<MeasurementService.XmlState> {

    data class XmlState(
        var startTokens: Long? = null,
        var startTimestamp: String? = null,
        var endTokens: Long? = null,
        var endTimestamp: String? = null,
    )

    private var xmlState = XmlState()

    val measurement: MeasurementState
        get() = MeasurementState(
            startTokens = xmlState.startTokens,
            startTimestamp = xmlState.startTimestamp,
            endTokens = xmlState.endTokens,
            endTimestamp = xmlState.endTimestamp,
        )

    fun setStart(tokens: Long, timestamp: String) {
        xmlState = XmlState(startTokens = tokens, startTimestamp = timestamp)
    }

    fun setEnd(tokens: Long, timestamp: String) {
        xmlState = xmlState.copy(endTokens = tokens, endTimestamp = timestamp)
    }

    fun clear() {
        xmlState = XmlState()
    }

    override fun getState(): XmlState = xmlState

    override fun loadState(state: XmlState) {
        xmlState = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): MeasurementService =
            ApplicationManager.getApplication().getService(MeasurementService::class.java)
    }
}
