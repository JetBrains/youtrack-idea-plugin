package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTrackerManualEntryDialog
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project


class ManualEntryAction(val repo: YouTrackServer, val project: Project) : IssueAction() {
    override val text = "Post work item manually"
    override val description = "Post work item manually"
    override val icon = AllIcons.Actions.Edit
    override val shortcut = "control shift I"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val dialog = TimeTrackerManualEntryDialog(project, repo)
            dialog.show()
            // TODO why not bubble
            val trackerNote = TrackerNotification()
            if (dialog.state == 200){
                trackerNote.notify("Time was successfully posted on server", NotificationType.INFORMATION)
                ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
            } else {
                trackerNote.notify("Time was not posted, please check your input", NotificationType.ERROR)
            }
        }
    }
}