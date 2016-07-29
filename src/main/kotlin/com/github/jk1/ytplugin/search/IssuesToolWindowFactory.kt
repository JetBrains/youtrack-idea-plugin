package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.YouTrackPluginIcons
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.components.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


class IssuesToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createContent(project, toolWindow)
        // this icon is loaded via IconLoader, thus adaptive
        toolWindow.icon = YouTrackPluginIcons.YOUTRACK_TOOL_WINDOW
    }

    private fun createContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val contentManager = toolWindow.contentManager
        contentManager.removeAllContents(false)
        val repos = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        if (repos.isEmpty()) {
            val panel = createPlaceholderPanel { createContent(project, toolWindow) }
            val content = contentFactory.createContent(panel, "No server found", false)
            content.isCloseable = false
            contentManager.addContent(content)
        }
        repos.forEach {
            val tabName = when {
                repos.size == 1 -> "Issues"
                else -> "Issues: ${it.url.split("//").last()}"
            }
            val panel = IssueListPanel(project, it, contentManager)
            val content = contentFactory.createContent(panel, tabName, false)
            content.isCloseable = false
            contentManager.addContent(content)
        }
    }

    private fun createPlaceholderPanel(refreshContentAction: () -> Unit): JComponent {
        val panel = JPanel(BorderLayout())
        val labelContainer = JPanel()
        val actionContainer = JPanel(FlowLayout())
        val messageLabel = JLabel("No YouTrack active repository found")
        val configureLabel = createLink("Configure", { configureRepository() })
        val refreshLabel = createLink("Refresh", refreshContentAction)
        messageLabel.alignmentX = Component.CENTER_ALIGNMENT
        actionContainer.add(configureLabel)
        actionContainer.add(refreshLabel)
        labelContainer.layout = BoxLayout(labelContainer, BoxLayout.Y_AXIS)
        labelContainer.add(messageLabel)
        labelContainer.add(actionContainer)
        labelContainer.maximumSize = messageLabel.preferredSize
        panel.add(labelContainer, BorderLayout.NORTH)
        return panel
    }

    private fun createLink(text: String, onClick: () -> Unit): JComponent {
        val label = JLabel()
        label.text = "<html><body><a href=\"\">$text</a></body></html>"
        label.cursor = Cursor(Cursor.HAND_CURSOR)
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onClick.invoke()
            }
        })
        return label
    }

    private fun configureRepository() {
        val action = ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID)
        val context = DataManager.getInstance().dataContext
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context)
        action.actionPerformed(event)
    }
}