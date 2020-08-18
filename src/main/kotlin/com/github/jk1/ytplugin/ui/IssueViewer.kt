package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.*
import com.github.jk1.ytplugin.ui.WikiHtmlPaneFactory.setHtml
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED


class IssueViewer : JPanel(BorderLayout()) {

    var currentIssue: Issue? = null
    private val rootPane = JPanel(BorderLayout())
    private lateinit var scrollToTop: () -> Unit

    init {
        val scrollPane = JBScrollPane(rootPane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        scrollPane.verticalScrollBar.unitIncrement = 16
        add(scrollPane, BorderLayout.CENTER)
        rootPane.isFocusable = true
    }

    fun showIssue(issue: Issue) {
        rootPane.removeAll()
        currentIssue = issue
        val container = JPanel()
        container.layout = BoxLayout(container, BoxLayout.PAGE_AXIS)
        rootPane.add(createHeaderPanel(issue), BorderLayout.NORTH)
        rootPane.add(container, BorderLayout.CENTER)
        if (issue.tags.isNotEmpty()) {
            container.add(createTagPanel(issue))
        }
        issue.links.groupBy { it.role }.forEach {
            container.add(createLinkPanel(it.key, it.value))
        }
        val issuePane = WikiHtmlPaneFactory.createHtmlPane(currentIssue!!)
        issuePane.border = BorderFactory.createEmptyBorder(0, 8, 5, 0)
        container.add(issuePane)
        val tabs = JBTabbedPane()
        addCommentsTab(issue.comments, tabs)
        addAttachmentsTab(issue.attachments, tabs)
        addWorkLogTab(issue.workItems, tabs)
        container.add(tabs)
        issuePane.setHtml(issue.description)
        scrollToTop.invoke()
        issuePane.repaint()
    }

    fun showWorkItems(issueWorkItem: IssueWorkItem) {
        rootPane.removeAll()
        val container = JPanel()
        container.layout = BoxLayout(container, BoxLayout.PAGE_AXIS)
        rootPane.add(createWorkItemPanel(issueWorkItem), BorderLayout.NORTH)

        scrollToTop.invoke()
        rootPane.repaint()
    }

    private fun createHeaderPanel(issue: Issue): JPanel {
        val panel = JPanel(BorderLayout())
        // todo: strikeout resolved issue ids
        val textArea = JTextArea()
        textArea.text = "${issue.id} ${issue.summary}"
        textArea.wrapStyleWord = true
        textArea.lineWrap = true
        textArea.isOpaque = false
        textArea.isEditable = false
        textArea.isFocusable = false
        textArea.background = UIManager.getColor("Label.background")
        textArea.border = BorderFactory.createEmptyBorder(2, 7, 0, 0)
        textArea.font = textArea.font.deriveFont(4.0f + textArea.font.size)
        panel.add(textArea, BorderLayout.CENTER)
        scrollToTop = { textArea.caretPosition = 0 }
        return panel
    }

    private fun createWorkItemPanel(issueWorkItem: IssueWorkItem): JPanel {
        val panel = JPanel(BorderLayout())
        // todo: strikeout resolved issue ids
        val textArea = JTextArea()
        textArea.text = "${issueWorkItem.id} ${issueWorkItem.comment}"
        textArea.wrapStyleWord = true
        textArea.lineWrap = true
        textArea.isOpaque = false
        textArea.isEditable = false
        textArea.isFocusable = false
        textArea.background = UIManager.getColor("Label.background")
        textArea.border = BorderFactory.createEmptyBorder(2, 7, 0, 0)
        textArea.font = textArea.font.deriveFont(4.0f + textArea.font.size)
        panel.add(textArea, BorderLayout.CENTER)
        scrollToTop = { textArea.caretPosition = 0 }
        return panel
    }

    private fun createTagPanel(issue: Issue): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = BorderFactory.createEmptyBorder(0, 4, 0, 0)
        issue.tags.forEach {
            val label = JLabel(" ${it.text} ")
            if (UIUtil.isUnderDarcula()) {
                val color = Color(200, 200, 200)
                label.foreground = color
                label.border = BorderFactory.createLineBorder(color)
            } else {
                label.foreground = it.foregroundColor
                label.background = it.backgroundColor
                label.border = BorderFactory.createLineBorder(it.backgroundColor, 2)
                label.isOpaque = true
            }
            panel.add(label)
        }
        return panel
    }

    private fun createLinkPanel(role: String, links: List<IssueLink>): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = BorderFactory.createEmptyBorder(0, 4, 0, 0)
        panel.add(JLabel("${role.capitalize()}: "))
        links.forEach { panel.add(HyperlinkLabel(it.value, it.url)) }
        return panel
    }

    private fun addAttachmentsTab(files: List<Attachment>, tabs: JBTabbedPane) {
        if (files.isNotEmpty()) {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
            panel.border = BorderFactory.createEmptyBorder(0, 15, 4, 0)
            files.forEach {
                val fileType = FileTypeManager.getInstance().getFileTypeByExtension(it.fileName.split(".").last())
                val link = HyperlinkLabel(it.fileName, it.url, fileType.icon)
                link.alignmentX = Component.RIGHT_ALIGNMENT
                panel.add(link)
            }
            tabs.addTab("Attachments (${files.size})", panel)
        }
    }

    private fun addCommentsTab(comments: List<IssueComment>, tabs: JBTabbedPane){
        if (comments.isNotEmpty()) {
            val commentsPanel = JPanel()
            commentsPanel.layout = BoxLayout(commentsPanel, BoxLayout.Y_AXIS)
            tabs.addTab("Comments (${comments.size})", commentsPanel)
            tabs.isFocusable = false
            comments.forEach { commentsPanel.add(createCommentPanel(it)) }
        }
    }

    private fun addWorkLogTab(workItems: MutableList<IssueWorkItem>, tabs: JBTabbedPane){
        if (workItems.isNotEmpty()) {
            val workItemsPanel = JPanel()
            workItemsPanel.layout = BoxLayout(workItemsPanel, BoxLayout.Y_AXIS)
            tabs.addTab("Work Items (${workItems.size})", workItemsPanel)
            tabs.isFocusable = false

            workItems.sort()
            workItems.forEach { workItemsPanel.add(createWorkItemsPanel(it)) }
        }
    }

    private fun createCommentPanel(comment: IssueComment): JPanel {
        val topPanel = JPanel(BorderLayout())
        val commentPanel = JPanel(BorderLayout())
        val header = SimpleColoredComponent()
        header.icon = AllIcons.General.User
        header.append(comment.authorName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        header.append(" at ")
        header.append(comment.created.format())
        topPanel.add(header, BorderLayout.WEST)
        val commentPane = WikiHtmlPaneFactory.createHtmlPane(currentIssue!!)
        commentPane.margin = Insets(2, 4, 0, 0)
        commentPane.setHtml(comment.text)
        commentPanel.add(commentPane, BorderLayout.CENTER)
        val panel = JPanel(BorderLayout())
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(commentPanel, BorderLayout.CENTER)
        panel.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        return panel
    }

    private fun createWorkItemsPanel(workItem: IssueWorkItem): JPanel {
        val workItemsPanel = JPanel(GridLayout(1, 8, 0, 0))

        val header = SimpleColoredComponent()
        header.icon = AllIcons.General.User
        header.append(workItem.author, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)

        val date = SimpleColoredComponent()
        date.append(workItem.date.format().substring(0, workItem.date.format().length - 6), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)

        val value = SimpleColoredComponent()
        value.append(workItem.value, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        value.icon = AllIcons.Vcs.History
        value.append("    ")

        val slash = SimpleColoredComponent()
        slash.append("                   |")
        val slash1 = SimpleColoredComponent()
        slash1.append("                  |")
        val slash2 = SimpleColoredComponent()
        slash2.append("                  |")

        workItemsPanel.add(header)
        workItemsPanel.add(slash)
        workItemsPanel.add(date)
        workItemsPanel.add(slash1)
        workItemsPanel.add(value)

        val comment = SimpleColoredComponent()
        if (workItem.comment != null){
            comment.append(workItem.comment, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            workItemsPanel.add(slash2)
            workItemsPanel.add(comment)
        }
        else{
            workItemsPanel.add(JLabel(""))  // for empty cell
            workItemsPanel.add(JLabel(""))  // for empty cell
        }

        workItemsPanel.add(JLabel(""))  // for empty cell

        val panel = JPanel(BorderLayout())
        panel.add(workItemsPanel, BorderLayout.NORTH)

        return panel
    }
}
