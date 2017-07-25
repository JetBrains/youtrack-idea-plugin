package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.Attachment
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueComment
import com.github.jk1.ytplugin.issues.model.IssueLink
import com.github.jk1.ytplugin.ui.WikiHtmlPaneFactory.setHtml
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

class IssueViewer(val project: Project) : JPanel(BorderLayout()) {

    var currentIssue: Issue? = null
    val rootPane = JPanel(BorderLayout())
    lateinit var scrollToTop: () -> Unit

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
        issuePane.border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        container.add(issuePane)
        val tabs = JBTabbedPane()
        addCommentsTab(issue.comments, tabs)
        addAttachmentsTab(issue.attachments, tabs)
        container.add(tabs)
        issuePane.setHtml(issue.description)
        scrollToTop.invoke()
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

    private fun createCommentPanel(comment: IssueComment): JPanel {
        val topPanel = JPanel(BorderLayout())
        val commentPanel = JPanel(BorderLayout())
        val header = SimpleColoredComponent()
        header.icon = AllIcons.Modules.Types.UserDefined
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
}