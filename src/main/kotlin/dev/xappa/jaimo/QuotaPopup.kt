package dev.xappa.jaimo

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.swing.*

object QuotaPopup {

    private const val BAR_WIDTH = 28
    private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()

    private val OK_COLOR = JBColor(Color(0x33, 0x99, 0x33), Color(0x55, 0xCC, 0x55))
    private val WARNING_COLOR = JBColor(Color(0xCC, 0x99, 0x00), Color(0xCC, 0xCC, 0x00))
    private val ERROR_COLOR = JBColor(Color(0xCC, 0x33, 0x33), Color(0xFF, 0x55, 0x55))

    fun show(state: QuotaState, component: Component, statusBar: StatusBar) {
        val panel = buildPanel(state)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel)
            .setRequestFocus(false)
            .setFocusable(false)
            .setMovable(false)
            .createPopup()

        val point = RelativePoint(component, Point(0, -popup.content.preferredSize.height))
        popup.show(point)
    }

    private fun buildPanel(state: QuotaState): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
            isOpaque = false
        }
        val mono = Font(Font.MONOSPACED, Font.PLAIN, 12)

        if (state.error != null) {
            panel.add(label("Error: ${state.error}", mono))
            return panel
        }

        val quota = state.quota
        if (quota == null) {
            panel.add(label("No quota data found", mono))
            return panel
        }

        val percent = (quota.current * 100 / quota.maximum).toInt()
        val color = colorForPercent(percent)
        val bar = buildBar(percent)

        panel.add(label("[${bar}] $percent%", mono, color))

        val usedFmt = NUMBER_FORMAT.format(quota.current)
        val maxFmt = NUMBER_FORMAT.format(quota.maximum)
        panel.add(label("Used: $usedFmt / $maxFmt", mono))

        val refill = state.refill
        if (refill != null) {
            val days = ChronoUnit.DAYS.between(LocalDate.now(), refill.date)
            panel.add(label("Refill: ${refill.date} ($days days)", mono))
        }

        return panel
    }

    private fun label(text: String, font: Font, color: Color? = null): JLabel =
        JLabel(text).apply {
            this.font = font
            if (color != null) foreground = color
            alignmentX = Component.LEFT_ALIGNMENT
        }

    private fun colorForPercent(percent: Int): Color = when {
        percent >= 80 -> ERROR_COLOR
        percent >= 50 -> WARNING_COLOR
        else -> OK_COLOR
    }

    // kept for tests
    fun buildText(state: QuotaState): String {
        if (state.error != null) return "Error: ${state.error}"
        val quota = state.quota ?: return "No quota data found"
        val percent = (quota.current * 100 / quota.maximum).toInt()
        val usedFmt = NUMBER_FORMAT.format(quota.current)
        val maxFmt = NUMBER_FORMAT.format(quota.maximum)
        val bar = buildBar(percent)
        val sb = StringBuilder()
        sb.appendLine("[$bar] $percent%")
        sb.appendLine("Used: $usedFmt / $maxFmt")
        val refill = state.refill
        if (refill != null) {
            val days = ChronoUnit.DAYS.between(LocalDate.now(), refill.date)
            sb.append("Refill: ${refill.date} ($days days)")
        }
        return sb.toString().trimEnd()
    }

    private fun buildBar(percent: Int): String {
        val filled = (percent * BAR_WIDTH / 100).coerceIn(0, BAR_WIDTH)
        val empty = BAR_WIDTH - filled
        return "#".repeat(filled) + "-".repeat(empty)
    }
}
