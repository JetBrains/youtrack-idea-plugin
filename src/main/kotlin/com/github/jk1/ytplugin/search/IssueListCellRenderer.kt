package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_BOLD
import com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.text.SimpleDateFormat
import javax.swing.*

class IssueListCellRenderer(val viewportWidthProvider: () -> Int) : JPanel(BorderLayout()), ListCellRenderer<Issue> {

    private val idSummary = SimpleColoredComponent()
    private val fields = SimpleColoredComponent()
    private val time = JLabel()
    private val glyphs = JLabel()
    private val font = when {
        SystemInfo.isMac -> "Lucida Grande"
        SystemInfo.isWindows -> "Arial"
        else -> "Verdana"
    }

    init {
        idSummary.isOpaque = false
        idSummary.font = Font(font, Font.PLAIN, 13)
        fields.font = Font(font, Font.PLAIN, 12)
        time.font = Font(font, Font.PLAIN, 10)
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)

        val top = createNonOpaquePanel()
        val bottom = createNonOpaquePanel()
        top.add(idSummary, BorderLayout.WEST)
        top.add(time, BorderLayout.EAST)
        bottom.add(fields, BorderLayout.WEST)
        bottom.add(glyphs, BorderLayout.EAST)
        add(top, BorderLayout.NORTH)
        add(bottom, BorderLayout.SOUTH)
    }

    private fun createNonOpaquePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        return panel
    }

    override fun getListCellRendererComponent(list: JList<out Issue>,
                                              issue: Issue, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        background = UIUtil.getListBackground(isSelected)
        fillSummaryLine(issue)
        fields.clear()
        fields.isOpaque = !isSelected
        fields.background = this.background
        issue.customFields.forEach {
            val attributes = when {
                isSelected || UIUtil.isUnderDarcula() -> SimpleTextAttributes(STYLE_PLAIN, null)
                else -> SimpleTextAttributes(it.backgroundColor, it.foregroundColor, null, STYLE_PLAIN)
            }
            fields.append(it.formatValues(), attributes)
            fields.append("   ")
        }
        time.foreground = if (isSelected) UIUtil.getListForeground(true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        time.text = SimpleDateFormat().format(issue.updateDate) + " "
        return this
    }

    private fun fillSummaryLine(issue: Issue){
        val viewportWidth = viewportWidthProvider.invoke() - 200    // leave some space for timestamp
        idSummary.clear()
        idSummary.icon = AllIcons.Toolwindows.ToolWindowDebugger
        idSummary.iconTextGap = 3
        idSummary.ipad = Insets(0, 4, 0, 0)
        idSummary.append(issue.id)
        idSummary.append(" ")
        val summaryWords = issue.summary.split(" ").iterator()
        // add summary words one by one until we hit viewport width limit
        while (summaryWords.hasNext() && (viewportWidth > idSummary.computePreferredSize(false).width)){
            idSummary.append(" ${summaryWords.next()}", SimpleTextAttributes(STYLE_BOLD, null))
        }
        if (summaryWords.hasNext()){
            idSummary.append(" …", SimpleTextAttributes(STYLE_BOLD, null))
        }
    }
}