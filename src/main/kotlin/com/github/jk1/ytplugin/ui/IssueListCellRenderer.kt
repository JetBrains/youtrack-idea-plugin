package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.*
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*

class IssueListCellRenderer(val viewportWidthProvider: () -> Int) : JPanel(BorderLayout()), ListCellRenderer<Issue> {

    private val topPanel = JPanel(BorderLayout())
    private val bottomPanel = JPanel(BorderLayout())
    private val idSummaryPanel = JPanel(BorderLayout())
    private val idSummary = SimpleColoredComponent()
    private val fields = SimpleColoredComponent()
    private val time = JLabel()
    private val glyphs = JLabel()

    var compactView: Boolean = true
        set(value) {
            if (value) remove(bottomPanel) else add(bottomPanel, BorderLayout.SOUTH)
            field = value
        }

    init {
        idSummary.isOpaque = false
        idSummaryPanel.isOpaque = false
        idSummary.font = Font(time.font.family, Font.PLAIN, 13)
        fields.font = Font(time.font.family, Font.PLAIN, 12)
        time.font = Font(time.font.family, Font.PLAIN, 10)
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        topPanel.isOpaque = false
        topPanel.add(idSummaryPanel, BorderLayout.WEST)
        topPanel.add(time, BorderLayout.EAST)
        bottomPanel.isOpaque = false
        bottomPanel.add(fields, BorderLayout.WEST)
        bottomPanel.add(glyphs, BorderLayout.EAST)
        add(topPanel, BorderLayout.NORTH)
        add(bottomPanel, BorderLayout.SOUTH)
    }

    override fun getListCellRendererComponent(list: JList<out Issue>,
                                              issue: Issue, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        val fgColor = when {
            isSelected -> UIUtil.getListForeground(true)
            issue.resolved -> Color(150, 150, 150)
            UIUtil.isUnderDarcula() -> Color(200, 200, 200)
            else -> Color(75, 107, 244)
        }
        background = UIUtil.getListBackground(isSelected)
        fillSummaryLine(issue, fgColor)
        fillCustomFields(issue, fgColor, isSelected)
        time.foreground = if (isSelected) UIUtil.getListForeground(true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        time.text = issue.updateDate.format() + " "
        return this
    }

    private fun fillSummaryLine(issue: Issue, fgColor: Color) {
        val viewportWidth = viewportWidthProvider.invoke() - 200    // leave some space for timestamp
        idSummaryPanel.removeAll()
        idSummary.clear()
        createIcon(issue)
        idSummary.ipad = Insets(0, 4, 0, 0)
        var idStyle = STYLE_BOLD
        if (issue.resolved) {
            idStyle = idStyle.or(STYLE_STRIKEOUT)
        }
        idSummary.append(issue.id, SimpleTextAttributes(idStyle, fgColor))
        idSummary.append(" ")
        val summaryWords = issue.summary.split(" ").iterator()
        // add summary words one by one until we hit viewport width limit
        while (summaryWords.hasNext() && (viewportWidth > idSummary.computePreferredSize(false).width)) {
            idSummary.append(" ${summaryWords.next()}", SimpleTextAttributes(STYLE_BOLD, fgColor))
        }
        if (summaryWords.hasNext()) {
            idSummary.append(" …", SimpleTextAttributes(STYLE_BOLD, fgColor))
        }
        idSummaryPanel.add(createIcon(issue), BorderLayout.WEST)
        idSummaryPanel.add(idSummary, BorderLayout.EAST)
    }

    private fun fillCustomFields(issue: Issue, fgColor: Color, isSelected: Boolean) {
        val viewportWidth = viewportWidthProvider.invoke() - 100
        fields.clear()
        fields.isOpaque = !isSelected
        fields.background = this.background
        issue.customFields.forEach {
            if (viewportWidth > fields.computePreferredSize(false).width) {
                val attributes = when {
                    isSelected || UIUtil.isUnderDarcula() -> SimpleTextAttributes(STYLE_PLAIN, fgColor)
                    else -> SimpleTextAttributes(it.backgroundColor, it.foregroundColor, null, STYLE_PLAIN)
                }
                fields.append(it.formatValues(), attributes)
                fields.append("   ")
            }
        }
    }

    private fun createIcon(issue: Issue): JComponent {
        val priorityField = issue.customFields.firstOrNull { "Priority" == it.name }
        if (!compactView || priorityField == null) {
            val label = JLabel(AllIcons.Toolwindows.ToolWindowDebugger)
            label.border = BorderFactory.createEmptyBorder(0, 3, 0, 0)
            return label
        } else {
            val label = JLabel(" ${priorityField.value.first().first()} ")
            label.background = priorityField.backgroundColor
            label.foreground = priorityField.foregroundColor
            label.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            label.isOpaque = !UIUtil.isUnderDarcula()
            return label
        }
    }
}