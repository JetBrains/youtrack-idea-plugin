package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Insets
import java.io.IOException
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit

class IssueViewer(val project: Project) : JPanel(BorderLayout()) {

    var browserPane: JTextPane = JTextPane()
    var currentIssue: Issue? = null

    init {
        val editorKit = HTMLEditorKit()
        val css = UIUtil.displayPropertiesToCSS(UIUtil.getLabelFont(), UIUtil.getLabelForeground())
        editorKit.styleSheet.addRule(css)
        browserPane.editorKit = editorKit
        browserPane.contentType = "text/html"
        browserPane.margin = Insets(0, 0, 0, 0)
        browserPane.isEditable = false
        val scroll = JBScrollPane(browserPane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED)
        add(scroll, BorderLayout.CENTER)
    }

    fun showIssue(issue: Issue) {
        currentIssue = issue
        val preview = generateHtml(issue)
        SwingUtilities.invokeLater { browserPane.text = preview }
    }

    private fun generateHtml(issue: Issue): String {
        val id = issue.id
        val summary = issue.summary
        val description = StringUtil.unescapeXml(issue.description)
        try {
            var main = loadResource("issue.html")
            val css = loadResource(if (UIUtil.isUnderDarcula()) "style_dark.css" else "style.css")
            val wikiCss = loadResource("wiki.css")
            main = StringUtil.replace(main,
                    arrayOf("{##STYLES}", "{##ID}", "{##Summary}", "{##Description}"),
                    arrayOf(css + wikiCss, id, summary, description))
            main = StringUtil.replace(main, "{##comments}", "")
            return main
        } catch (e: IOException) {
            // todo: fallback to plaintext and log
            e.printStackTrace()
        }
        return ""
    }

    private fun loadResource(name: String) = FileUtil.loadTextAndClose(javaClass.getResourceAsStream(name))
}