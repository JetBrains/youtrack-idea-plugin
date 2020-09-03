package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.tasks.TaskManager
import java.util.*
import javax.swing.Icon
import javax.swing.ImageIcon


class StopTrackerAction(val timer: TimeTracker) : IssueAction() {
    override val text = "Stop work timer"
    override val description = "Stop work timer"
    override var icon: Icon = ImageIcon(this::class.java.classLoader.getResource("icons/time_tracker_stop_dark.png"))
    override val shortcut = "control shift L"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            val repo = project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }

            val time = timer.stop()

            val bar = project?.let { it1 -> WindowManager.getInstance().getStatusBar(it1) }
            bar?.removeWidget("Time Tracking Clock")

            val trackerNote = TrackerNotification()

            if (time == "0")
                trackerNote.notify("Time was not recorded (less than 1 minute)", NotificationType.ERROR)
            else{
                val status = repo?.let { it1 ->
                    TimeTrackerRestClient(it1).postNewWorkItem(timer.issueId,
                            timer.getRecordedTime(), timer.type, timer.getComment(), (Date().time).toString())
                }
                if (status != 200)
                    trackerNote.notify("Could not record time: time tracking is disabled", NotificationType.ERROR)
                else{
                    trackerNote.notify("Work timer stopped, time posted on server", NotificationType.INFORMATION)
                    ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                }
            }
        }
    }
}