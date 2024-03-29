package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager

class HelpAction : IssueAction() {
    override val text = "Help"
    override val description = "Learn more about the YouTrack Integration plugin"
    override val icon = AllIcons.Actions.Help
    override val shortcut = "control shift H"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val url = "https://www.jetbrains.com/help/youtrack/standalone/YouTrack-Integration-Plugin.html"
            ApplicationManager.getApplication().getService(BrowserLauncher::class.java).open(url)

        }
    }
}