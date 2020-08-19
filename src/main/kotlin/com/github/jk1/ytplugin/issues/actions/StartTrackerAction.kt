package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager

class StartTrackerAction : IssueAction() {
    override val text = "Start time tracking"
    override val description = "Start time tracking"
    override var icon = AllIcons.Actions.Profile
    override val shortcut = "control shift K"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val url = "https://www.jetbrains.com/help/youtrack/standalone/YouTrack-Integration-Plugin.html"
            ServiceManager.getService(BrowserLauncher::class.java).open(url)
        }
    }
}