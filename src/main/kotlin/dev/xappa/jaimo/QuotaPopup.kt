package dev.xappa.jaimo

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

object QuotaPopup {

    private const val BAR_WIDTH = 28
    private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()
    private val TIMESTAMP_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    private val OK_COLOR = JBColor(Color(0x33, 0x99, 0x33), Color(0x55, 0xCC, 0x55))
    private val WARNING_COLOR = JBColor(Color(0xCC, 0x99, 0x00), Color(0xCC, 0xCC, 0x00))
    private val ERROR_COLOR = JBColor(Color(0xCC, 0x33, 0x33), Color(0xFF, 0x55, 0x55))

    fun show(state: QuotaState, previousQuota: Long?, component: Component, statusBar: StatusBar) {
        val text = buildText(state, previousQuota)
        val color = state.quota?.let {
            colorForPercent((it.current * 100 / it.maximum).toInt())
        }
        val panel = buildPanel(text, color)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel)
            .setRequestFocus(true)
            .setFocusable(true)
            .setMovable(false)
            .createPopup()

        val point = RelativePoint(component, Point(0, -popup.content.preferredSize.height))
        popup.show(point)
    }

    private fun buildPanel(text: String, barColor: Color?): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
            isOpaque = false
        }

        val textPane = JTextPane().apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            isEditable = false
            isOpaque = false
            border = null
        }

        val doc = textPane.styledDocument
        val defaultStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, UIManager.getColor("Label.foreground") ?: textPane.foreground)
        }

        if (barColor != null) {
            val firstLineEnd = text.indexOf('\n').let { if (it < 0) text.length else it }
            val colorStyle = SimpleAttributeSet().apply {
                StyleConstants.setForeground(this, barColor)
            }
            doc.insertString(0, text.substring(0, firstLineEnd), colorStyle)
            if (firstLineEnd < text.length) {
                doc.insertString(doc.length, text.substring(firstLineEnd), defaultStyle)
            }
        } else {
            doc.insertString(0, text, defaultStyle)
        }

        panel.add(textPane, BorderLayout.CENTER)
        return panel
    }

    fun buildText(state: QuotaState, previousQuota: Long? = null): String {
        if (state.error != null) return "Error: ${state.error}"
        val quota = state.quota ?: return "No quota data found"
        val percent = (quota.current * 100 / quota.maximum).toInt()
        val usedFmt = NUMBER_FORMAT.format(quota.current)
        val maxFmt = NUMBER_FORMAT.format(quota.maximum)
        val bar = buildBar(percent)
        val sb = StringBuilder()
        sb.appendLine("[$bar] $percent%")
        sb.appendLine("Used:       $usedFmt / $maxFmt")
        val refill = state.refill
        if (refill != null) {
            val days = ChronoUnit.DAYS.between(LocalDate.now(), refill.date)
            sb.appendLine("Refill:     ${refill.date} ($days days)")
        }
        sb.appendLine("Updated:    ${formatTimestamp(quota.timestamp)}")
        if (previousQuota != null) {
            val delta = quota.current - previousQuota
            val sign = if (delta >= 0) "+" else ""
            sb.append("Since last: $sign${NUMBER_FORMAT.format(delta)}")
        }
        return sb.toString().trimEnd()
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dt = LocalDateTime.parse(timestamp, TIMESTAMP_PARSER)
            val time = dt.format(TIME_FORMAT)
            val secondsAgo = ChronoUnit.SECONDS.between(dt, LocalDateTime.now())
            val refreshInSeconds = (QuotaService.REFRESH_INTERVAL_MS / 1000) + (QuotaService.REFRESH_BUFFER_MS / 1000) - secondsAgo
            val relative = when {
                refreshInSeconds <= 0 -> "refreshing soon"
                refreshInSeconds < 60 -> "refresh in <1m"
                else -> "refresh in ${refreshInSeconds / 60}m"
            }
            "$time ($relative)"
        } catch (_: Exception) {
            timestamp
        }
    }

    private fun colorForPercent(percent: Int): Color = when {
        percent >= 80 -> ERROR_COLOR
        percent >= 50 -> WARNING_COLOR
        else -> OK_COLOR
    }

    private fun buildBar(percent: Int): String {
        val filled = (percent * BAR_WIDTH / 100).coerceIn(0, BAR_WIDTH)
        val empty = BAR_WIDTH - filled
        return "#".repeat(filled) + "-".repeat(empty)
    }
}
