package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TimeTrackerManualEntryDialog
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.tasks.TaskManager
import javax.swing.Icon
import javax.swing.ImageIcon


class ManualEntryAction(val repo: YouTrackServer, val project: Project) : IssueAction() {
    override val text = "Post work item manually"
    override val description = "Post work item manually"
    override val icon = AllIcons.Actions.Edit
    override val shortcut = "control shift I"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            TimeTrackerManualEntryDialog(project, repo).show()
        }
    }
}