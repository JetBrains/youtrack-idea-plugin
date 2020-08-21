package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.intellij.icons.AllIcons
import com.intellij.tasks.youtrack.YouTrackRepository
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
        private val viewportWidthProvider: () -> Int, repo: YouTrackRepository) : JPanel(BorderLayout()), ListCellRenderer<IssueWorkItem> {

    private val myRepository = repo
    private val topPanel = JPanel(BorderLayout())
    private val summaryPanel = JPanel(BorderLayout())
    private val trackingComments = SimpleColoredComponent()

    private val complimentaryColor = Color(123, 123, 127)
    private val fgColor = Color(75, 107, 244)
    private val idStyle = STYLE_BOLD


    init {
        summaryPanel.isOpaque = false
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        topPanel.isOpaque = false
        topPanel.add(summaryPanel, BorderLayout.WEST)
        add(topPanel, BorderLayout.NORTH)
    }

    override fun getListCellRendererComponent(list: JList<out IssueWorkItem>,
                                              issueWorkItem: IssueWorkItem, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        background = UIUtil.getListBackground(false)
        fillTrackingInfoLine(issueWorkItem, fgColor)
        return this
    }

    private fun fillTrackingInfoLine(issueWorkItem: IssueWorkItem, fgColor: Color) {
        summaryPanel.removeAll()

        val author = SimpleColoredComponent()
        author.isOpaque = false
        author.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
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

        val issueLink = HyperlinkLabel(issueWorkItem.issueId,
                "${myRepository.url}/issue/${issueWorkItem.issueId}")

        prepareCommentsForDisplaying(issueWorkItem)

        val panel = JPanel(GridLayout(1, 6, 0, 0))
        panel.isOpaque = false
        panel.preferredSize = Dimension(7 * viewportWidthProvider.invoke() / 10, viewportWidthProvider.invoke() / 50)
        panel.add(author)
        panel.add(date)
        panel.add(value)
        panel.add(issueLink)
        panel.add(trackingComments)

        summaryPanel.add(panel, BorderLayout.CENTER)
    }

    private fun prepareCommentsForDisplaying(issueWorkItem: IssueWorkItem) {
        val viewportWidth = viewportWidthProvider.invoke() / 10

        trackingComments.clear()
        trackingComments.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        trackingComments.isOpaque = false

        val comments = issueWorkItem.comment?.split(" ")?.iterator()
        if (comments != null) {
            while (comments.hasNext() && (viewportWidth > trackingComments.computePreferredSize(false).width)) {
                trackingComments.append(" ${comments.next()}", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
        if (comments != null) {
            if (comments.hasNext()) {
                trackingComments.append(" …", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
    }

}