package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.intellij.icons.AllIcons
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.lang.Integer.max
import java.lang.Math.min
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer


class WorkItemsListCellRenderer(
        private val viewportWidthProvider: () -> Int, repo: YouTrackRepository) : JPanel(BorderLayout()), ListCellRenderer<IssueWorkItem> {

    private val myRepository = repo
    private val topPanel = JPanel(BorderLayout())
    private val summaryPanel = JPanel(BorderLayout())
    private val trackingComments = SimpleColoredComponent()
    private lateinit var issueLink: HyperlinkLabel

    private val complimentaryColor = Color(123, 123, 127)
    private val idStyle = STYLE_PLAIN

    lateinit var datePanel: JPanel
    lateinit var valuePanel: JPanel

    private fun getValuePanelPosition() = valuePanel.preferredSize.getWidth()
    private fun getDatePanelPosition() = datePanel.preferredSize.getWidth()

    private var maxIssueIdWidth = 0


    fun getIssuePosition(): List<Int> {
        val panelWidth = viewportWidthProvider.invoke()
        //returns start of the x-position of issueId and its length
        return listOf((getDatePanelPosition() + getValuePanelPosition()).toInt(), (0.2 * panelWidth).toInt())
    }

    init {
        summaryPanel.isOpaque = false
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        topPanel.isOpaque = false
        topPanel.add(summaryPanel, BorderLayout.WEST)
        add(topPanel, BorderLayout.NORTH)
        topPanel.revalidate()
    }

    override fun getListCellRendererComponent(list: JList<out IssueWorkItem>,
                                              issueWorkItem: IssueWorkItem, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        background = UIUtil.getListBackground(false, false)
        fillTrackingInfoLine(issueWorkItem)
        return this
    }

    private fun fillTrackingInfoLine(issueWorkItem: IssueWorkItem) {

        summaryPanel.removeAll()
        summaryPanel.isOpaque = false

        val date = SimpleColoredComponent()
        date.isOpaque = false
        date.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)

        val sdf = SimpleDateFormat("MMM dd yyyy")
        sdf.timeZone = if (issueWorkItem.timeZone != "") TimeZone.getTimeZone(issueWorkItem.timeZone) else TimeZone.getDefault()
        date.append(sdf.format(issueWorkItem.date), SimpleTextAttributes(idStyle, complimentaryColor))

        val value = SimpleColoredComponent()
        value.isOpaque = false
        value.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        value.icon = AllIcons.Vcs.History
        value.append(issueWorkItem.value, SimpleTextAttributes(idStyle, complimentaryColor))

        val type = SimpleColoredComponent()
        type.isOpaque = false
        type.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        type.append(if (issueWorkItem.type == "None") "No type" else issueWorkItem.type, SimpleTextAttributes(idStyle, complimentaryColor))

        issueLink = HyperlinkLabel(issueWorkItem.issueId,
                "${myRepository.url}/issue/${issueWorkItem.issueId}", AllIcons.Actions.MoveTo2)
        issueLink.isOpaque = false

        if (issueWorkItem.issueId.length > maxIssueIdWidth) {
            maxIssueIdWidth = issueWorkItem.issueId.length
        }

        prepareCommentsForDisplaying(issueWorkItem)

        val layout = FlowLayout(FlowLayout.LEFT)
        layout.hgap = 0
        layout.vgap = 0
        val panel = JPanel(layout)
        panel.isOpaque = false

        val panelWidth = viewportWidthProvider.invoke()
        val panelHeight = 32

        panel.preferredSize = Dimension(panelWidth, panelHeight)

        datePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        datePanel.preferredSize = Dimension((0.156 * panelWidth).toInt(), panelHeight + 10)
        datePanel.add(date)
        datePanel.isOpaque = false

        value.alignmentX = Component.RIGHT_ALIGNMENT
        valuePanel = JPanel(FlowLayout(FlowLayout.LEFT))

        valuePanel.preferredSize = Dimension((0.313 * panelWidth).toInt(), panelHeight + 10)
        valuePanel.alignmentX = Component.RIGHT_ALIGNMENT
        valuePanel.isOpaque = false

        val issueLinkPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        issueLinkPanel.add(issueLink)
        issueLinkPanel.isOpaque = false

        val minIssueIdWidth = (0.078 * panelWidth).toInt()
        val unitWidth = 0.078 * panelWidth / 6

        if (panelWidth > 1000) {
            datePanel.preferredSize = Dimension((0.156 * panelWidth).toInt(), panelHeight + 10)
            valuePanel.preferredSize = Dimension((0.313 * panelWidth).toInt(), panelHeight + 10)
            issueLinkPanel.preferredSize = Dimension(max((unitWidth * maxIssueIdWidth).toInt(), minIssueIdWidth), panelHeight + 10)

        } else {
            datePanel.preferredSize = Dimension((0.3 * panelWidth).toInt(), panelHeight)
            valuePanel.preferredSize = Dimension((0.4 * panelWidth).toInt(), panelHeight)
            issueLinkPanel.preferredSize = Dimension((0.2 * panelWidth).toInt(), panelHeight)
        }

        datePanel.add(date)
        valuePanel.add(value)
        panel.add(datePanel)
        panel.add(valuePanel)

        if (panelWidth > 500) {

            panel.add(issueLinkPanel)

            if (panelWidth > 1000) {
                val typePanel = JPanel(FlowLayout(FlowLayout.LEFT))
                typePanel.add(type)
                typePanel.preferredSize = Dimension((0.156 * panelWidth).toInt(), panelHeight + 10)
                typePanel.isOpaque = false
                panel.add(typePanel)

                val trackingCommentsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
                trackingCommentsPanel.add(trackingComments)
                val commentsWidth = (0.274 * panelWidth).toInt() +
                        minIssueIdWidth - (unitWidth * maxIssueIdWidth).toInt()
                trackingCommentsPanel.preferredSize = Dimension(min(commentsWidth, (0.274 * panelWidth).toInt()), panelHeight + 10)

                trackingCommentsPanel.isOpaque = false
                panel.add(trackingCommentsPanel)
            }
        }

        summaryPanel.add(panel, BorderLayout.CENTER)
    }


    private fun prepareCommentsForDisplaying(issueWorkItem: IssueWorkItem) {
        val viewportWidth = viewportWidthProvider.invoke() / 8

        trackingComments.clear()
        trackingComments.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        trackingComments.isOpaque = false

        val comments = issueWorkItem.comment?.split(" ")?.iterator()
        if (comments != null && issueWorkItem.comment != "") {
            trackingComments.icon = AllIcons.General.Balloon

            while (comments.hasNext() && (viewportWidth > trackingComments.computePreferredSize(false).width)) {
                trackingComments.append(" ${comments.next()}", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
        if (comments != null && issueWorkItem.comment != "") {
            if (comments.hasNext()) {
                trackingComments.append(" …", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
    }

}
