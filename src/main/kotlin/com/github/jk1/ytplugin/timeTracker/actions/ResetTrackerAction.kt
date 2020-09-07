package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.IconLoader
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ResetTrackerAction  : AnAction(
        "Reset work timer",
        "Reset work timer",
        IconLoader.loadIcon("icons/time_tracker_reset_dark.png")){

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            if (project != null) {
                val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
                val myTimer = ComponentAware.of(event.project!!).timeTrackerComponent[repo]

                if (myTimer.isRunning) {
                    myTimer.isRunning = false
                    myTimer.isPaused = false
                    myTimer.recordedTime = "0"
                    myTimer.timeInMills = 0
                    myTimer.startTime = System.currentTimeMillis()
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Work timer reset", NotificationType.INFORMATION)
                } else {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Could not reset - timer is not started", NotificationType.ERROR)
                }
            }
        }
    }
}
