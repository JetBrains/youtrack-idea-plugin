package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetUsagesCollector
import java.lang.IllegalStateException
import java.util.*


class StopTrackerAction : AnAction(
        "Stop work timer",
        "Post recorded time to YouTrack server and reset timer",
        YouTrackPluginIcons.YOUTRACK_STOP_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            stopTimer(project)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isEnabled = timer.isRunning
            event.presentation.isVisible = (timer.isManualTrackingEnable || timer.isAutoTrackingEnable)
        }
    }

    fun stopTimer(project: Project) {
        val trackerNote = TrackerNotification()

        val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
        val timer = ComponentAware.of(project).timeTrackerComponent

        try {
            timer.stop()
            val bar = project.let { it1 -> WindowManager.getInstance().getStatusBar(it1) }
            bar?.removeWidget("Time Tracking Clock")

            println("time: " + timer.recordedTime)
            if (timer.recordedTime == "0")
                trackerNote.notify("Time was not recorded (less than 1 minute)", NotificationType.WARNING)
            else {
                val status = repo.let { it1 ->
                    TimeTrackerRestClient(it1).postNewWorkItem(timer.issueId,
                            timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
                }
                if (status != 200)
                    trackerNote.notify("Could not record time: time tracking is disabled", NotificationType.WARNING)
                else {
                    trackerNote.notify("Work timer stopped, time ${timer.recordedTime} " +
                            "posted on server for issue ${timer.issueIdReadable}", NotificationType.INFORMATION)
                    ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                }
            }
        } catch (e: IllegalStateException) {
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.WARNING)
        }

    }
}