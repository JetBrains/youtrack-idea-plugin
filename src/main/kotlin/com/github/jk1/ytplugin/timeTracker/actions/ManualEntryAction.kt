package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.IconLoader
import com.github.jk1.ytplugin.timeTracker.TimeTrackerManualEntryDialog
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ManualEntryAction  : AnAction(
        "Add spent time",
        "Add spent time",
        IconLoader.loadIcon("icons/add_new_dark_16.png")){

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
                trackerNote.notify("Unable to add spent time manually" , NotificationType.ERROR)
            }
        }
    }
}