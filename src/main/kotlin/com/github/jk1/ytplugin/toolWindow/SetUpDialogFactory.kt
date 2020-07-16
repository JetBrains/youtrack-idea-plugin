package com.github.jk1.ytplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


/**
 * Create the tool window content.
 * @author Alina Boshchenko
 */
class SetUpDialogFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = SetUpDialog(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.content, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
