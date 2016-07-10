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
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit

class MessageBrowser(val project: Project) : JPanel(BorderLayout()) {

    var myBrowser: JTextPane = JTextPane()

    fun showIssue(issue: Issue) {

        myBrowser = JTextPane()
        myBrowser.margin = Insets(0, 0, 0, 0)
        val editorKit = HTMLEditorKit()
        myBrowser.isEditable = false
        editorKit.styleSheet.addRule(UIUtil.displayPropertiesToCSS(UIUtil.getLabelFont(), UIUtil.getLabelForeground()))
        myBrowser.editorKit = editorKit
        myBrowser.contentType = "text/html"
        add(JBScrollPane(myBrowser, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER)
        revalidate()
        repaint()

        val s = generateHtml(issue)
        SwingUtilities.invokeLater { myBrowser.setText(s) }
    }

    private val STACKTRACE_LINE = Pattern.compile("[\t]*at [[_a-zA-Z0-9]+\\.]+[_a-zA-Z$0-9]+\\.([a-zA-Z$0-9_]+|<init>)\\(([[[A-Za-z0-9_]+\\.java:[\\d]+]]+|[Native\\sMethod]+|[Unknown\\sSource]+)\\)+[ [~]*\\[[a-zA-Z0-9\\.\\:/]\\]]*")

    private fun generateHtml(issue: Issue): String {
        var id = issue.id
        val summary = issue.summary
        val description = html(StringUtil.unescapeXml(issue.description))

        try {
            var main = FileUtil.loadTextAndClose(MessageBrowser::class.java.getResourceAsStream("issue.html"))
            val css = FileUtil.loadTextAndClose(MessageBrowser::class.java.getResourceAsStream(if (UIUtil.isUnderDarcula()) "style_dark.css" else "style.css"))
            val url = "https://elle.myjetbrains.com/youtrack/issue/TEST-1"
            id = "<a href='$url'>$id</a>"
            main = StringUtil.replace(main, arrayOf("{##STYLES}", "{##ID}", "{##Summary}", "{##Description}"), arrayOf(css, id, summary, description))

            main = StringUtil.replace(main, "{##comments}", "")
            return main
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ""
    }

    private fun html(description: String?): String {
        if (description == null) {
            return ""
        }
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
                if (ind  < linkEnd && ind > 0) {
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