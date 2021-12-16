package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager

/**
 * Opens currently selected issue in a browser.
 * This is about tool window selection, not about an active task.
 */
class BrowseIssueAction(private val getSelectedIssue: () -> Issue?) : IssueAction() {

    override val text = "View in YouTrack"
    override val description = "View the selected issue in the YouTrack web application"
    override val icon = AllIcons.General.Web
    override val shortcut = "control shift B"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val issue = getSelectedIssue.invoke()
            // youtrack issues always have a url defined
            if (issue != null) {
                logger.debug("Opening ${issue.id} browser: ${issue.url}")
                ApplicationManager.getApplication().getService(BrowserLauncher::class.java).open(issue.url)

            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}