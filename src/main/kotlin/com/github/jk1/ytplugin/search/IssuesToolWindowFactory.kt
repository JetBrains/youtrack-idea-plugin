package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.YouTrackPluginIcons
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class IssuesToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val contentManager = toolWindow.contentManager
        ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach {
            val tabName = "Issues: ${it.url.split("//").last()}"
            val panel = IssueListPanel(project, it, contentManager)
            val content = contentFactory.createContent(panel, tabName, false)
            content.isCloseable = false
            contentManager.addContent(content)
        }

        // this icon is loaded via IconLoader, thus adaptive
        toolWindow.icon = YouTrackPluginIcons.YOUTRACK_TOOL_WINDOW
    }
}