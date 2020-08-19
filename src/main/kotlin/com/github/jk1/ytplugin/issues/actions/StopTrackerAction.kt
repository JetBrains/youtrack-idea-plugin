package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager

class StopTrackerAction : IssueAction() {
    override val text = "Stop time tracking"
    override val description = "Stop time tracking"
    override var icon = AllIcons.Actions.Suspend
    override val shortcut = "control shift L"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val url = "https://www.jetbrains.com/help/youtrack/standalone/YouTrack-Integration-Plugin.html"
            ServiceManager.getService(BrowserLauncher::class.java).open(url)
        }
    }
}