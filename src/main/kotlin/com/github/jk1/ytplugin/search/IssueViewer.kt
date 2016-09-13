package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.format
import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.search.model.Issue
import com.github.jk1.ytplugin.search.model.IssueComment
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLEditorKit

class IssueViewer(val project: Project) : JPanel(BorderLayout()) {

    var currentIssue: Issue? = null
    val rootPane = JPanel()
    val scrollPane: JBScrollPane
    val issuePane = createHtmlPane()
    lateinit var scrollToTop: () -> Unit

    init {
        rootPane.layout = BoxLayout(rootPane, BoxLayout.Y_AXIS)
        scrollPane = JBScrollPane(rootPane, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER)
        add(scrollPane, BorderLayout.CENTER)
    }

    fun showIssue(issue: Issue) {
        rootPane.removeAll()
        currentIssue = issue
        val header = createHeaderPanel(issue)
        rootPane.add(header)
        if (issue.tags.isNotEmpty()) {
            rootPane.add(createTagPanel(issue))
        }
        issue.links.groupBy { it.role }.forEach {
            rootPane.add(createLinkPanel(it.key, it.value.map { it.value }))
        }
        rootPane.add(issuePane)
        if (issue.comments.isNotEmpty()) {
            val tabsPane = JBTabbedPane()
            val commentsPanel = JPanel()
            commentsPanel.layout = BoxLayout(commentsPanel, BoxLayout.Y_AXIS)
            tabsPane.addTab("Comments", commentsPanel)
            issue.comments.forEach { commentsPanel.add(createCommentPanel(it)) }
            rootPane.add(tabsPane)
        }
        issuePane.text = generateHtml(issue)
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
        textArea.font = Font("arial", Font.PLAIN, 18)
        panel.add(textArea, BorderLayout.CENTER)
        panel.maximumSize = issuePane.size
        scrollToTop = { textArea.caretPosition = 0 }
        // rootPane.addComponentListener(ResizeListener { panel.maximumSize = issuePane.size })
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
                label.isOpaque = true
            }
            panel.add(label)
        }
        return panel
    }

    private fun createLinkPanel(role: String, links: List<String>): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = BorderFactory.createEmptyBorder(0, 4, 0, 0)
        panel.add(JLabel("${role.capitalize()}: ${links.joinToString(", ")}"))
        return panel
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
        val commentPane = createHtmlPane()
        commentPane.margin = Insets(2, 4, 0, 0)
        commentPane.text = comment.text
        commentPanel.add(commentPane, BorderLayout.CENTER)
        val panel = JPanel(BorderLayout())
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(commentPanel, BorderLayout.CENTER)
        panel.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        return panel
    }

    private fun createHtmlPane(): JTextPane {
        val htmlPane = JTextPane()
        val editorKit = HTMLEditorKit()
        val rules = UIUtil.displayPropertiesToCSS(UIUtil.getLabelFont(), UIUtil.getLabelForeground())
        editorKit.styleSheet.addRule(rules)
        htmlPane.editorKit = editorKit
        htmlPane.contentType = "text/html"
        htmlPane.isEditable = false
        htmlPane.addHyperlinkListener {
            if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                val url = "${currentIssue?.repoUrl}${it.description}"
                BrowserLauncher.getInstance().open(url)
            }
        }
        return htmlPane
    }

    private fun generateHtml(issue: Issue): String {
        try {
            return ResourceTemplate("issue.html")
                    .put("styles", loadStyles())
                    .put("description", StringUtil.unescapeXml(issue.description))
                    .render()
        } catch (e: Exception) {
            logger.warn("Issue rendering failed", e)
            return "<html><body>An error occurred during issue rendering. Check IDE log for more details.</body></html>"
        }
    }

    private fun loadStyles() = loadResource(if (UIUtil.isUnderDarcula()) "style_dark.css" else "style.css") +
            loadResource("wiki.css")

    private fun loadResource(name: String) = FileUtil.loadTextAndClose(javaClass.getResourceAsStream(name))

    inner class ResizeListener(val action: () -> Unit) : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            action.invoke()
        }
    }
}