package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.*
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import javax.swing.*
import javax.swing.border.EmptyBorder

class IssueListCellRenderer() : JPanel(BorderLayout()), ListCellRenderer<Issue> {

    private val idSummary = SimpleColoredComponent()
    private val fields = SimpleColoredComponent()
    private val time = JLabel()
    private val glyphs = JLabel()
    private val font = if (SystemInfo.isMac) "Lucida Grande" else if (SystemInfo.isWindows) "Arial" else "Verdana"

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
        idSummary.clear()
        idSummary.append(issue.id)
        idSummary.append(" ")
        idSummary.append(issue.summary, SimpleTextAttributes(STYLE_BOLD, null))
        idSummary.icon = AllIcons.Toolwindows.ToolWindowDebugger
        fields.clear()
        fields.isOpaque = !isSelected
        fields.background = this.background
        issue.customFields.forEach {
            val attributes = when {
                isSelected -> SimpleTextAttributes(STYLE_PLAIN, null)
                else -> SimpleTextAttributes(it.backgroundColor, it.foregroundColor, null, STYLE_PLAIN)
            }
            fields.append(it.formatValues(), attributes)
            fields.append("   ")
        }
        time.foreground = if (isSelected) UIUtil.getListForeground(true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        time.text = SimpleDateFormat().format(issue.updateDate) + " "
        return this
    }
}