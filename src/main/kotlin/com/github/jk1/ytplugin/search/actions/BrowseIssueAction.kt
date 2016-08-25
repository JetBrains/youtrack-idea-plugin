package com.github.jk1.ytplugin.search.actions

import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * Opens currently selected issue in a browser.
 * This is about tool window selection, not about an active task.
 */
class BrowseIssueAction(val getSelectedIssue: () -> Issue?) : AnAction(
        "Open in Browser",
        "Opens selected YouTrack issue in your favorite browser",
        AllIcons.General.Web), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            val task = getSelectedIssue.invoke()?.asTask()
            // youtrack issues always have a url defined
            if (task != null) {
                logger.debug("Opening ${task.id} browser: ${task.issueUrl}")
                BrowserLauncher.getInstance().open(task.issueUrl!!)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}