package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.timeTracker.IconLoader
import com.github.jk1.ytplugin.timeTracker.TimeTrackerManualEntryDialog
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.net.UnknownHostException
import javax.swing.JLabel


class ManualEntryAction  : AnAction(
        "Add spent time",
        "Add spent time",
        IconLoader.loadIcon("icons/add_new_dark_16.png")){

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            val repo = project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }

            try{
                val dialog = repo?.let { it1 -> TimeTrackerManualEntryDialog(project, it1) }
                if (dialog != null){
                    dialog.show()
                } else {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Unable to add spent time manually" , NotificationType.WARNING)
                }
            }
            catch (e: UnknownHostException){
                val trackerNote = TrackerNotification()
                trackerNote.notify("Network error, please check your connection", NotificationType.WARNING)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null ) {
            try {
                val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
                event.presentation.isVisible = repo.getRepo().isConfigured
            }
            catch(e: NoActiveYouTrackTaskException) {
                event.presentation.isVisible = false
            }
        }
    }
}