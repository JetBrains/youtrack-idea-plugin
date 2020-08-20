package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.intellij.icons.AllIcons
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_BOLD
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*


class WorkItemsListCellRenderer(
        private val viewportWidthProvider: () -> Int) : JPanel(BorderLayout()), ListCellRenderer<IssueWorkItem> {

    private val topPanel = JPanel(BorderLayout())
    private val bottomPanel = JPanel(BorderLayout())
    private val summaryPanel = JPanel(BorderLayout())
    private val author = SimpleColoredComponent()
    private val trackingComments = SimpleColoredComponent()
    private val time = JLabel()


    init {
        author.isOpaque = false
        summaryPanel.isOpaque = false
        author.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        trackingComments.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size)
        time.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size - 2 )
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        topPanel.isOpaque = false
        topPanel.add(summaryPanel, BorderLayout.WEST)
        topPanel.add(time, BorderLayout.EAST)
        bottomPanel.isOpaque = false
        bottomPanel.add(trackingComments, BorderLayout.WEST)
        bottomPanel.border = BorderFactory.createEmptyBorder(3, 0, 0, 0)
        add(topPanel, BorderLayout.NORTH)
        add(bottomPanel, BorderLayout.SOUTH)
    }

    override fun getListCellRendererComponent(list: JList<out IssueWorkItem>,
                                              issueWorkItem: IssueWorkItem, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        val fgColor = Color(75, 107, 244)

        background = UIUtil.getListBackground(isSelected)
        fillTrackingInfoLine(issueWorkItem, fgColor)
        fillComments(issueWorkItem, isSelected)
        time.foreground = if (isSelected) UIUtil.getListForeground(true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        time.text = issueWorkItem.created.format() + " "
        return this
    }

    private fun fillTrackingInfoLine(issueWorkItem: IssueWorkItem, fgColor: Color) {
        val complimentaryColor =Color(123, 123, 127)
        summaryPanel.removeAll()

        author.clear()
        val idStyle = STYLE_BOLD
        author.icon = AllIcons.General.User
        author.append(issueWorkItem.author, SimpleTextAttributes(idStyle, fgColor))

        val date = SimpleColoredComponent()
        date.isOpaque = false
        date.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        date.append(issueWorkItem.date.format().substring(0, issueWorkItem.date.format().length - 5), SimpleTextAttributes(idStyle, complimentaryColor))

        val value = SimpleColoredComponent()
        value.isOpaque = false
        value.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        value.icon = AllIcons.Vcs.History
        value.append(issueWorkItem.value, SimpleTextAttributes(idStyle, fgColor))

        val issue = SimpleColoredComponent()
        issue.isOpaque = false
        issue.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        issue.append("Issue " + issueWorkItem.issueId, SimpleTextAttributes(idStyle, complimentaryColor))

        val panel = JPanel(GridLayout(1, 6, 0, 0))
        panel.isOpaque = false
        panel.preferredSize = Dimension(1050, 40)
        panel.add(author)
        panel.add(date)
        panel.add(value)
        panel.add(issue)
        panel.add(JLabel(""))

        summaryPanel.add(panel, BorderLayout.CENTER)
    }

    private fun fillComments(issueWorkItem: IssueWorkItem, isSelected: Boolean) {
        val viewportWidth = viewportWidthProvider.invoke() - 200    // leave some space for timestamp
        trackingComments.clear()
        trackingComments.isOpaque = !isSelected
        trackingComments.background = this.background
        val comments = issueWorkItem.comment?.split(" ")?.iterator()

        if (comments != null) {
            while (comments.hasNext() && (viewportWidth > author.computePreferredSize(false).width)) {
                trackingComments.append(" ${comments.next()}")
            }
        }
        if (comments != null) {
            if (comments.hasNext()) {
                trackingComments.append(" …")
            }
        }
    }

}