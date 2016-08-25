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
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Insets
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLEditorKit

class IssueViewer(val project: Project) : JPanel(BorderLayout()) {

    var currentIssue: Issue? = null
    val rootPane = JPanel()
    val issuePane = createHtmlPane()

    init {
        rootPane.layout = BoxLayout(rootPane, BoxLayout.Y_AXIS)
        add(JBScrollPane(rootPane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER)
    }

    fun showIssue(issue: Issue) {
        val preview = generateHtml(issue)
        currentIssue = issue
        SwingUtilities.invokeLater {
            rootPane.removeAll()
            rootPane.add(issuePane)
            if (issue.comments.isNotEmpty()) {
                issuePane.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY)
            }
            issue.comments.forEach { rootPane.add(createCommentPanel(it)) }
            issuePane.text = preview
            issuePane.caretPosition = 0
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
                    .put("id", issue.id)
                    .put("summary", issue.summary)
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
}