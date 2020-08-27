package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.ClockWidget
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

class StartTrackerAction(val repo: YouTrackServer, timer: TimeTracker, val project: Project, val taskManager: TaskManager) : IssueAction() {
    override val text = "Start work timer"
    override val description = "Start work timer"
    override var icon = AllIcons.Actions.Profile
    override val shortcut = "control shift K"
    private val myTimer = timer


    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val activeTask = taskManager.activeTask
            if (!myTimer.isRunning || myTimer.isPaused) {
                myTimer.issueId = IssuesRestClient(repo).getEntityIdByIssueId(activeTask.id)
                if (myTimer.issueId == "0"){
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Could not post time: not a YouTrack issue", NotificationType.ERROR)
                } else {
                    val bar = WindowManager.getInstance().getStatusBar(project)
                    if (bar?.getWidget("Time Tracking Clock") == null){
                        bar?.addWidget(ClockWidget(myTimer))
                    }
                    myTimer.start(activeTask.id)
                }
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Work timer is already running", NotificationType.ERROR)
            }
        }
    }
}