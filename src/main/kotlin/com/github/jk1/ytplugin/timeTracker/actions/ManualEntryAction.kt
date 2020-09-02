package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.timeTracker.TimeTrackerManualEntryDialog
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware


class ManualEntryAction : AnAction(
        "Post work item manually",
        "Post work item manually",
        AllIcons.Actions.Edit), DumbAware, IdeNotificationsTrait {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            val repo = project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }

            val dialog = repo?.let { it1 -> TimeTrackerManualEntryDialog(project, it1) }
            if (dialog != null){
                dialog.show()
                // TODO why not bubble
                val trackerNote = TrackerNotification()
                if (dialog.state == 200){
                    trackerNote.notify("Time was successfully posted on server", NotificationType.INFORMATION)
                    ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                } else {
                    trackerNote.notify("Time was not posted, please check your input", NotificationType.ERROR)
                }
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Unable add work item manually" , NotificationType.ERROR)
            }
        }
    }
}