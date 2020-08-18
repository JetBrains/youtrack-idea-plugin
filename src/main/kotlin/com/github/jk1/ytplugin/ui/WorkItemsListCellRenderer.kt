package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
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

class WorkItemsListCellRenderer(
        private val viewportWidthProvider: () -> Int) : JPanel(BorderLayout()), ListCellRenderer<IssueWorkItem> {

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
        idSummary.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
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

    override fun getListCellRendererComponent(list: JList<out IssueWorkItem>,
                                              issueWorkItem: IssueWorkItem, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        val fgColor =Color(75, 107, 244)

        background = UIUtil.getListBackground(isSelected)
        fillSummaryLine(issueWorkItem, fgColor)
        fillCustomFields(issueWorkItem, fgColor, isSelected)
        time.foreground = if (isSelected) UIUtil.getListForeground(true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        time.text = issueWorkItem.created.format() + " "
        return this
    }

    private fun fillSummaryLine(issueWorkItem: IssueWorkItem, fgColor: Color) {
        val viewportWidth = viewportWidthProvider.invoke() - 200    // leave some space for timestamp
        idSummaryPanel.removeAll()
        idSummary.clear()
        idSummary.ipad = Insets(0, 4, 0, 0)
        val idStyle = STYLE_BOLD
        idSummary.icon = AllIcons.General.User
        idSummary.append(issueWorkItem.author, SimpleTextAttributes(idStyle, fgColor))
        idSummary.append("      ")
        idSummary.append(issueWorkItem.date.format().substring(0, issueWorkItem.date.format().length - 5)
                + "      ", SimpleTextAttributes(idStyle, fgColor))
        idSummary.append(issueWorkItem.value, SimpleTextAttributes(idStyle, fgColor))

        idSummaryPanel.add(idSummary, BorderLayout.EAST)
    }

    private fun fillCustomFields(issueWorkItem: IssueWorkItem, fgColor: Color, isSelected: Boolean) {
        val viewportWidth = viewportWidthProvider.invoke() - 200    // leave some space for timestamp
        fields.clear()
        fields.isOpaque = !isSelected
        fields.background = this.background
        val summaryWords = issueWorkItem.comment?.split(" ")?.iterator()

        if (summaryWords != null) {
            while (summaryWords.hasNext() && (viewportWidth > idSummary.computePreferredSize(false).width)) {
                fields.append(" ${summaryWords.next()}")
            }
        }
        if (summaryWords != null) {
            if (summaryWords.hasNext()) {
                fields.append(" …")
            }
        }
    }

}