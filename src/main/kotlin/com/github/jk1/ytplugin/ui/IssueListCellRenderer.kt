package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.*
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*

class IssueListCellRenderer(
        private val viewportWidthProvider: () -> Int,
        private val iconProvider: IssueListCellIconProvider) : JPanel(BorderLayout()), ListCellRenderer<Issue> {

    private val topPanel = JPanel(BorderLayout())
    private val bottomPanel = JPanel(BorderLayout())
    private val idSummaryPanel = JPanel(BorderLayout())
    private val fields = SimpleColoredComponent()
    private val time = JLabel()
    private val glyphs = JLabel()

    var compactView: Boolean = true
        set(value) {
            if (value) remove(bottomPanel) else add(bottomPanel, BorderLayout.SOUTH)
            field = value
        }

    init {
        idSummaryPanel.isOpaque = false
        fields.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size)
        time.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size - 2 )
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        topPanel.isOpaque = false
        topPanel.add(idSummaryPanel, BorderLayout.WEST)
        topPanel.add(time, BorderLayout.EAST)
        bottomPanel.isOpaque = false
        bottomPanel.add(fields, BorderLayout.WEST)
        bottomPanel.add(glyphs, BorderLayout.EAST)
        bottomPanel.border = BorderFactory.createEmptyBorder(3, 0, 0, 0)
        add(topPanel, BorderLayout.NORTH)
        add(bottomPanel, BorderLayout.SOUTH)
    }

    override fun getListCellRendererComponent(list: JList<out Issue>,
                                              issue: Issue, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        val fgColor = when {
            isSelected -> UIUtil.getListForeground(true, true)
            issue.resolved -> Color(150, 150, 150)
            UIUtil.isUnderDarcula() -> Color(200, 200, 200)
            else -> Color(75, 107, 244)
        }
        background = UIUtil.getListBackground(isSelected, cellHasFocus)
        fillSummaryLine(issue, fgColor)
        fillCustomFields(issue, fgColor, isSelected)
        time.foreground = if (isSelected) UIUtil.getListForeground(true, true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        time.text = issue.updateDate.format() + " "
        return this
    }

    private fun fillSummaryLine(issue: Issue, fgColor: Color) {
        // add summary words one by one until we hit viewport width limit
        val viewportWidth = viewportWidthProvider.invoke() - 130    // leave some space for timestamp
        val wordCount = issue.summary.split(" ").size
        var idSummary = issue.toIdSummary(0, fgColor)
        for (i in 1..wordCount) {
            val incremented = issue.toIdSummary(i, fgColor)
            if (incremented.computePreferredSize(false).width < viewportWidth) {
                idSummary = incremented
            } else break
        }
        idSummaryPanel.removeAll()
        idSummaryPanel.add(iconProvider.createIcon(issue, compactView), BorderLayout.WEST)
        idSummaryPanel.add(idSummary, BorderLayout.EAST)
    }

    private fun Issue.toIdSummary(limit: Int = Int.MAX_VALUE, fgColor: Color): SimpleColoredComponent{
        val idSummary = SimpleColoredComponent()
        idSummary.isOpaque = false
        idSummary.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        idSummary.ipad = Insets(0, 4, 0, 0)
        var idStyle = STYLE_BOLD
        if (this.resolved) {
            idStyle = idStyle.or(STYLE_STRIKEOUT)
        }
        idSummary.append(this.id, SimpleTextAttributes(idStyle, fgColor))
        idSummary.append(" ")
        val words = this.summary.split(" ")
        words.take(limit).forEach { word ->
            idSummary.append(" ").append(word, SimpleTextAttributes(STYLE_BOLD, fgColor))
        }
        if (limit < words.size) {
            idSummary.append(" …", SimpleTextAttributes(STYLE_BOLD, fgColor))
        }
        return idSummary
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


}