package com.github.jk1.ytplugin.toolWindow

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.runAction
import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.ui.SetupListToolWindowContent
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities


/**
 * Create the tool window content.
 * @author Alina Boshchenko
 */
class SetUpDialogFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createContent(project, toolWindow)
        // listen to task management plugin configuration changes and update tool window accordingly
        ComponentAware.of(project).taskManagerComponent.addConfigurationChangeListener {
            SwingUtilities.invokeLater {
                logger.debug("Server configuration change detected, reloading tool window contents")
                createContent(project, toolWindow)
            }
        }
        // listen to resize events and convert from horizontal to vertical layout and back
        toolWindow.component.addComponentListener(object: ComponentAdapter(){

            private var horizontal = toolWindow.anchor.isHorizontal

            override fun componentResized(e: ComponentEvent) {
                if ((e.component.width > e.component.height).xor(horizontal)) {
                    horizontal = !horizontal
                    createContent(project, toolWindow)
                }
            }
        })
    }

//    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
//        val myToolWindow = SetUpDialog(project)
//        val contentFactory = ContentFactory.SERVICE.getInstance()
//        val content = contentFactory.createContent(myToolWindow.content, "", false)
//        toolWindow.contentManager.addContent(content)
//
//    }


    //ok
    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(YouTrackPluginIcons.MY_YOUTRACK_TOOL_WINDOW) // loaded via IconLoader, thus adaptive
    }

    private fun createContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        contentManager.removeAllContents(true)
        val repos = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        logger.debug("${repos.size} YouTrack repositories discovered")
        when {
            repos.isEmpty() -> contentManager.addContent("", createPlaceholderPanel(project))
            else -> {
                repos.forEach {
                    val panel = SetupListToolWindowContent(!toolWindow.anchor.isHorizontal, it)
                    contentManager.addContent("Issues | ${it.url.split("//").last()}", panel)
                }
                Disposer.register(project, Disposable {
                    contentManager.removeAllContents(true)
                })
            }
        }
    }

    //ok
    private fun ContentManager.addContent(title: String, component: JComponent){
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(component, title, false)
        content.isCloseable = false
        addContent(content)
    }

    //ok
    private fun createPlaceholderPanel(project: Project): JComponent {
        val panel = JPanel(BorderLayout())
        val labelContainer = JPanel()
        val messageLabel = JLabel("No YouTrack server found")
        val configureLabel = createLink("Configure") { SetupWindow(project) }
        messageLabel.alignmentX = Component.CENTER_ALIGNMENT
        configureLabel.alignmentX = Component.CENTER_ALIGNMENT
        labelContainer.add(messageLabel)
        labelContainer.add(configureLabel)
        panel.add(labelContainer, BorderLayout.NORTH)
        return panel
    }

    //ok
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
