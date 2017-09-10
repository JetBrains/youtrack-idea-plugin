package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.components.ServiceManager
import com.intellij.util.ui.UIUtil
import java.net.MalformedURLException
import java.net.URI
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.html.HTMLEditorKit

object WikiHtmlPaneFactory {

    private val editorKit = HTMLEditorKit()

    init {
        val rules = UIUtil.displayPropertiesToCSS(UIUtil.getLabelFont(), UIUtil.getLabelForeground())
        editorKit.styleSheet.importStyleSheet(javaClass.getResource("wiki.css"))
        editorKit.styleSheet.addRule(rules)
    }

    fun createHtmlPane(issue: Issue): JTextPane {
        val htmlPane = JTextPane()
        htmlPane.editorKit = editorKit
        htmlPane.contentType = "text/html"
        htmlPane.isEditable = false
        htmlPane.addHyperlinkListener(EventListener(issue))
        htmlPane.isFocusable = false
        return htmlPane
    }

    fun JTextPane.setHtml(html: String){
        text = "<html><body>$html</body></html>"
    }

    class EventListener(private val issue: Issue) : HyperlinkListener {

        override fun hyperlinkUpdate(event: HyperlinkEvent) {
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                ServiceManager.getService(BrowserLauncher::class.java).open(event.absoluteUrl)
            }
        }

        private val HyperlinkEvent.absoluteUrl: String get() {
            try {
                val uri = URI(description)
                if (uri.isAbsolute) {
                    return description
                }
            } catch(e: MalformedURLException) {
                logger.debug("Unable to parse $description as URI, will try to prefix it with YouTrack server address")
            }
            return "${issue.repoUrl}$description"
        }
    }
}