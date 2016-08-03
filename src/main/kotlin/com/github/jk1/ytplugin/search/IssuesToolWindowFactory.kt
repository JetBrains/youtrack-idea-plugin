package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.YouTrackPluginIcons
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.components.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.github.jk1.ytplugin.common.runAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


class IssuesToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createContent(project, toolWindow)
        toolWindow.icon = YouTrackPluginIcons.YOUTRACK_TOOL_WINDOW // loaded via IconLoader, thus adaptive
        ComponentAware.of(project).taskManagerComponent.addListener {
            // listen to task management plugin configuration changes and update tool window accordingly
            SwingUtilities.invokeLater {
                createContent(project, toolWindow)
            }
        }
    }

    private fun createContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        contentManager.removeAllContents(false)
        val repos = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        when {
            repos.size == 0 -> contentManager.addContent("No server found", createPlaceholderPanel())
            repos.size == 1 -> {
                val repo = repos.first()
                val panel = IssueListToolWindowContent(project, repo, contentManager)
                contentManager.addContent("Issues | ${repo.defaultSearch}", panel)
            }
            else -> {
                repos.forEach {
                    val panel = IssueListToolWindowContent(project, it, contentManager)
                    contentManager.addContent("Issues | ${it.url.split("//").last()}", panel)
                }
            }
        }
    }

    private fun ContentManager.addContent(title: String, component: JComponent){
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(component, title, false)
        content.isCloseable = false
        addContent(content)
    }

    private fun createPlaceholderPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val labelContainer = JPanel()
        val messageLabel = JLabel("No YouTrack server found")
        val configureLabel = createLink("Configure", { CONFIGURE_SERVERS_ACTION_ID.runAction() })
        messageLabel.alignmentX = Component.CENTER_ALIGNMENT
        configureLabel.alignmentX = Component.CENTER_ALIGNMENT
        labelContainer.add(messageLabel)
        labelContainer.add(configureLabel)
        panel.add(labelContainer, BorderLayout.NORTH)
        return panel
    }

    private fun createLink(text: String, onClick: () -> Unit): JComponent {
        val label = SimpleColoredComponent()
        label.append(text, SimpleTextAttributes.LINK_ATTRIBUTES)
        label.cursor = Cursor(Cursor.HAND_CURSOR)
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onClick.invoke()
            }
        })
        return label
    }
}