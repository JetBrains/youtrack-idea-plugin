package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.intellij.icons.AllIcons
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.lang.Integer.max
import java.lang.Math.min
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer


class WorkItemsListCellRenderer(
    private val viewportWidthProvider: () -> Int, repo: YouTrackRepository
) : JPanel(BorderLayout()), ListCellRenderer<IssueWorkItem> {

    private val myRepository = repo
    private val topPanel = JPanel(BorderLayout())
    private val summaryPanel = JPanel(BorderLayout())
    lateinit var trackingComments: SimpleColoredComponent
    private lateinit var issueLink: HyperlinkLabel

    lateinit var datePanel: JPanel
    lateinit var valuePanel: JPanel
    lateinit var issueLinkPanel: JPanel

    private fun getValuePanelPosition() = valuePanel.preferredSize.getWidth()
    private fun getDatePanelPosition() = datePanel.preferredSize.getWidth()

    private var PREFFERED_COMMENT_WIDTH = 0.374
    private val PREFFERED_ATTRIBUTE_WIDTH = 0.080

    private val PREFFERED_DATE_TYPE_WIDTH = 0.156
    private val PREFFERED_ISSUE_ID_WIDTH = 0.078
    private val PREFFERED_VALUE_WIDTH = 0.113

    private val OFFSET = 10
    private val LARGE_SCREEN_SIZE = 1000
    private val SMALL_SCREEN_SIZE = 450
    private val PREFFERED_PANEL_HEIGHT = 32

    private var maxAttributes = 0
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

    override fun getListCellRendererComponent(
        list: JList<out IssueWorkItem>,
        issueWorkItem: IssueWorkItem, index: Int,
        isSelected: Boolean, cellHasFocus: Boolean
    ): Component {

        background = UIUtil.getListBackground(false, false)
        fillTrackingInfoLine(issueWorkItem)
        return this
    }


    private fun fillTrackingInfoLine(issueWorkItem: IssueWorkItem) {

        summaryPanel.removeAll()
        summaryPanel.isOpaque = false

        val renderer = WorkItemCellRenderer(issueWorkItem)
        val date = renderer.fillDateComponent()
        val value = renderer.fillIssueComponent()
        val type = renderer.fillTypeComponent()
        val attributes = renderer.fillAttributesComponents()

        if (issueWorkItem.attributes.size > maxAttributes) {
            maxAttributes = issueWorkItem.attributes.size
            PREFFERED_COMMENT_WIDTH = 0.374 - maxAttributes * PREFFERED_ATTRIBUTE_WIDTH
            updateUI()
        }
        issueLink = HyperlinkLabel(
            issueWorkItem.issueId,
            "${myRepository.url}/issue/${issueWorkItem.issueId}", AllIcons.Actions.MoveTo2
        )
        issueLink.isOpaque = false

        if (issueWorkItem.issueId.length > maxIssueIdWidth) {
            maxIssueIdWidth = issueWorkItem.issueId.length
        }

        val viewportWidth = viewportWidthProvider.invoke() / 4
        trackingComments = renderer.fillCommentComponent(viewportWidth)

        val layout = FlowLayout(FlowLayout.LEFT)
        layout.hgap = 0
        layout.vgap = 0

        val panel = JPanel(layout)
        panel.isOpaque = false
        val panelWidth = viewportWidthProvider.invoke()
        panel.preferredSize = Dimension(panelWidth, PREFFERED_PANEL_HEIGHT)

        datePanel = createPanel()
        issueLinkPanel = createPanel()

        valuePanel = createPanel()
        value.alignmentX = Component.RIGHT_ALIGNMENT
        valuePanel.alignmentX = Component.RIGHT_ALIGNMENT

        if (panelWidth > LARGE_SCREEN_SIZE) {
            adaptSizeForTheLargeScreen(panelWidth)
        } else {
            adaptSizeForTheSmallScreen(panelWidth)
        }

        datePanel.add(date)
        valuePanel.add(value)
        issueLinkPanel.add(issueLink)

        panel.add(datePanel)
        panel.add(valuePanel)

        if (panelWidth > SMALL_SCREEN_SIZE) {
            panel.add(issueLinkPanel)
            if (panelWidth > LARGE_SCREEN_SIZE) {
                addType(type, panel, panelWidth)
                addAttributes(attributes, panel, panelWidth)
                addComment(panel, panelWidth)
            }
        }
        summaryPanel.add(panel, BorderLayout.CENTER)
    }

    private fun createPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.isOpaque = false
        return panel
    }

    private fun adaptSizeForTheLargeScreen(panelWidth: Int) {
        val minIssueIdWidth = (PREFFERED_ISSUE_ID_WIDTH * panelWidth).toInt()
        val unitWidth = PREFFERED_ISSUE_ID_WIDTH * panelWidth / 6

        datePanel.preferredSize = Dimension(
            (PREFFERED_DATE_TYPE_WIDTH * panelWidth).toInt(),
            PREFFERED_PANEL_HEIGHT + OFFSET
        )
        valuePanel.preferredSize = Dimension(
            (PREFFERED_VALUE_WIDTH * panelWidth).toInt(),
            PREFFERED_PANEL_HEIGHT + OFFSET
        )
        issueLinkPanel.preferredSize = Dimension(
            max((unitWidth * maxIssueIdWidth).toInt(), minIssueIdWidth),
            PREFFERED_PANEL_HEIGHT + OFFSET
        )
    }

    private fun adaptSizeForTheSmallScreen(panelWidth: Int) {
        datePanel.preferredSize = Dimension((0.3 * panelWidth).toInt(), PREFFERED_PANEL_HEIGHT)
        valuePanel.preferredSize = Dimension((0.4 * panelWidth).toInt(), PREFFERED_PANEL_HEIGHT)
        issueLinkPanel.preferredSize = Dimension((0.2 * panelWidth).toInt(), PREFFERED_PANEL_HEIGHT)
    }

    private fun addAttributes(attributes: List<SimpleColoredComponent>, panel: JPanel, panelWidth: Int) {
        attributes.forEach {
            val attributePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            attributePanel.add(it)
            attributePanel.preferredSize = Dimension(
                (PREFFERED_ATTRIBUTE_WIDTH * panelWidth).toInt(),
                PREFFERED_PANEL_HEIGHT + OFFSET
            )
            attributePanel.isOpaque = false
            panel.add(attributePanel)
        }

        // added space to be equal with the max number of attributes
        if (attributes.size < maxAttributes) {
            val attributePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            attributePanel.add(SimpleColoredComponent())
            attributePanel.preferredSize = Dimension(
                ((maxAttributes - attributes.size) *
                        PREFFERED_ATTRIBUTE_WIDTH * panelWidth).toInt(), PREFFERED_PANEL_HEIGHT + OFFSET
            )
            attributePanel.isOpaque = false
            panel.add(attributePanel)
        }
    }

    private fun addType(type: SimpleColoredComponent, panel: JPanel, panelWidth: Int) {
        val typePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        typePanel.add(type)
        typePanel.preferredSize = Dimension(
            (PREFFERED_DATE_TYPE_WIDTH * panelWidth).toInt(),
            PREFFERED_PANEL_HEIGHT + OFFSET
        )
        typePanel.isOpaque = false
        panel.add(typePanel)

    }

    private fun addComment(panel: JPanel, panelWidth: Int) {
        val minIssueIdWidth = (PREFFERED_ISSUE_ID_WIDTH * panelWidth).toInt()
        val unitWidth = PREFFERED_ISSUE_ID_WIDTH * panelWidth / 6

        val trackingCommentsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        trackingCommentsPanel.add(trackingComments)
        val commentsWidth = (PREFFERED_COMMENT_WIDTH * panelWidth).toInt() +
                minIssueIdWidth - (unitWidth * maxIssueIdWidth).toInt()
        trackingCommentsPanel.preferredSize = Dimension(
            min(
                commentsWidth,
                (PREFFERED_COMMENT_WIDTH * panelWidth).toInt()
            ), PREFFERED_PANEL_HEIGHT + OFFSET
        )

        trackingCommentsPanel.isOpaque = false
        panel.add(trackingCommentsPanel)
    }

}
