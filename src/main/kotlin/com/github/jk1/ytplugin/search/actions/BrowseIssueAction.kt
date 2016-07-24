package com.github.jk1.ytplugin.search.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.tasks.Task


class BrowseIssueAction(val getSelectedTask: () -> Task?) : AnAction(
        "Open in Browser",
        "Opens selected YouTrack issue in your favorite browser",
        AllIcons.General.Web) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            val task = getSelectedTask.invoke()
            // youtrack issues always have a url defined
            if (task != null) {
                BrowserLauncher.getInstance().open(task.issueUrl!!)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedTask.invoke() != null
    }
}