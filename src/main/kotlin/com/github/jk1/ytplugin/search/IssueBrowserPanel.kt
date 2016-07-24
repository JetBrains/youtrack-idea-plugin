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
import java.util.regex.Pattern
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit

class IssueBrowserPanel(val project: Project) : JPanel(BorderLayout()) {

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

    private val STACKTRACE_LINE = Pattern.compile("[\t]*at [[_a-zA-Z0-9]+\\.]+[_a-zA-Z$0-9]+\\.([a-zA-Z$0-9_]+|<init>)\\(([[[A-Za-z0-9_]+\\.java:[\\d]+]]+|[Native\\sMethod]+|[Unknown\\sSource]+)\\)+[ [~]*\\[[a-zA-Z0-9\\.\\:/]\\]]*")

    private fun generateHtml(issue: Issue): String {
        val id = issue.id
        val summary = issue.summary
        val description = html(StringUtil.unescapeXml(issue.description))
        try {
            var main = FileUtil.loadTextAndClose(IssueBrowserPanel::class.java.getResourceAsStream("issue.html"))
            val css = FileUtil.loadTextAndClose(IssueBrowserPanel::class.java.getResourceAsStream(if (UIUtil.isUnderDarcula()) "style_dark.css" else "style.css"))
            main = StringUtil.replace(main, arrayOf("{##STYLES}", "{##ID}", "{##Summary}", "{##Description}"), arrayOf(css, id, summary, description))
            main = StringUtil.replace(main, "{##comments}", "")
            return main
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ""
    }

    private fun html(description: String): String {
        val buf = StringBuffer()
        var listStarted = false
        var preStarted = false
        val split = StringUtil.split(description, "\n")
        split.forEach { s ->
            if (STACKTRACE_LINE.matcher(s).matches()) {
                if (!preStarted) {
                    buf.append("<pre>")
                    preStarted = true
                }
                //s = s.trim { it <= ' ' }
                val fqnStart = s.indexOf("at ") + 3
                val linkStart = s.indexOf('(') + 1
                val linkEnd = s.indexOf(')')
                var line = "-1"
                val ind: Int
                ind = s.indexOf(':', linkStart)
                if (ind < linkEnd && ind > 0) {
                    line = s.substring(s.indexOf(':') + 1, linkEnd)
                }
                var fqn = s.substring(fqnStart, linkStart)
                if (fqn.lastIndexOf('.') > 0) {
                    fqn = fqn.substring(0, fqn.lastIndexOf('.'))
                }
                if (fqn.lastIndexOf('$') > 0) {
                    fqn = fqn.substring(0, fqn.lastIndexOf('$'))
                }
                buf.append("  ")
                buf.append(s.substring(0, linkStart))
                buf.append("<a href='ide://$fqn/$line'>")
                buf.append(s.substring(linkStart, linkEnd))
                buf.append("</a>")
                buf.append(s.substring(linkEnd))
                buf.append("\n")
            } else {
                if (preStarted) {
                    preStarted = false
                    buf.append("</pre>")
                }
                if (s.startsWith("- ")) {
                    if (!listStarted) {
                        buf.append("<ul>\n")
                        listStarted = true
                    }
                    buf.append("<li>").append(s.substring(2)).append("</li>\n")
                } else {
                    if (listStarted) {
                        listStarted = false
                        buf.append("</ul>\n")
                    }
                    buf.append(s).append("<br/>")
                }
            }
        }
        return buf.toString()
    }

}