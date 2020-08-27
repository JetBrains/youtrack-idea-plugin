package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.timeTracker.TrackerNotifier
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.tasks.TaskManager
import javax.swing.Icon
import javax.swing.ImageIcon

class ResetTrackerAction(val repo: YouTrackServer, timer: TimeTracker, val project: Project, val taskManager: TaskManager) : IssueAction() {
    override val text = "Reset work timer"
    override val description = "Reset work timer"
    override var icon: Icon = ImageIcon(this::class.java.classLoader.getResource("icons/time_tracker_reset_dark.png"))
    override val shortcut = "control shift N"
    private val myTimer = timer

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            if (myTimer.isRunning){
                myTimer.isRunning = false
                myTimer.isPaused = false
                val bar = WindowManager.getInstance().getStatusBar(project)
                bar?.removeWidget("Time Tracking Clock")
                val trackerNote = TrackerNotification()
                trackerNote.notify("Work timer reset", NotificationType.INFORMATION)
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Could not reset - timer is not started", NotificationType.ERROR)
            }
        }
    }
}
