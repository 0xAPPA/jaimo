package dev.xappa.jaimo

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseEvent
import com.intellij.util.Consumer

class QuotaStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "AI Quota"

    override fun createWidget(project: Project): StatusBarWidget = QuotaStatusBarWidget()

    companion object {
        const val WIDGET_ID = "dev.xappa.jaimo.widget"
    }
}

class QuotaStatusBarWidget : StatusBarWidget, StatusBarWidget.TextPresentation, QuotaStateListener {

    private var statusBar: StatusBar? = null
    private val service = QuotaService.getInstance()

    override fun ID(): String = QuotaStatusBarWidgetFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        service.addListener(this)
        service.refresh()
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun dispose() {
        service.removeListener(this)
    }

    // TextPresentation

    override fun getText(): String {
        val quota = service.state.quota ?: return "AI: --"
        val percent = (quota.current * 100 / quota.maximum).toInt()
        return "AI: $percent%"
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getTooltipText(): String = "JetBrains AI Quota Usage"

    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer<MouseEvent> { e ->
        val sb = statusBar ?: return@Consumer
        QuotaPopup.show(service.state, service.previousQuota, e.component, sb)
    }

    // QuotaStateListener

    override fun onStateChanged(state: QuotaState) {
        statusBar?.updateWidget(QuotaStatusBarWidgetFactory.WIDGET_ID)
    }

    companion object {
        private val WARNING_COLOR = JBColor(Color(0xCC, 0x99, 0x00), Color(0xCC, 0xCC, 0x00))
        private val ERROR_COLOR = JBColor(Color(0xCC, 0x33, 0x33), Color(0xFF, 0x55, 0x55))

        fun colorForPercent(percent: Int): Color? = when {
            percent >= 80 -> ERROR_COLOR
            percent >= 50 -> WARNING_COLOR
            else -> null
        }
    }
}
