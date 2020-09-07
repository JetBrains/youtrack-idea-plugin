package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.timeTracker.IconLoader
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.util.*


class StopTrackerAction : AnAction(
        "Stop work timer",
        "Stop work timer",
        IconLoader.loadIcon("icons/time_tracker_stop_dark.png")){

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            if (project != null) {
                stopTimer(project)
            }
        }
    }

    fun stopTimer(project: Project){
        val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
        val timer = ComponentAware.of(project).timeTrackerComponent

        val time = timer.stop()

        val bar = project.let { it1 -> WindowManager.getInstance().getStatusBar(it1) }
        bar?.removeWidget("Time Tracking Clock")

        val trackerNote = TrackerNotification()

        if (time == "0")
            trackerNote.notify("Time was not recorded (less than 1 minute)", NotificationType.ERROR)
        else {
            val status = repo.let { it1 ->
                TimeTrackerRestClient(it1).postNewWorkItem(timer.issueId,
                        timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
            }
            if (status != 200)
                trackerNote.notify("Could not record time: time tracking is disabled", NotificationType.ERROR)
            else {
                trackerNote.notify("Work timer stopped, time posted on server", NotificationType.INFORMATION)
                ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
            }
        }
    }
}