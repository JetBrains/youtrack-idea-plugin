package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import javax.swing.Icon
import javax.swing.ImageIcon


class StopTrackerAction(timer: TimeTracker, repo: YouTrackServer, project: Project, val taskManager: TaskManager) : IssueAction() {
    override val text = "Stop work timer"
    override val description = "Stop work timer"
    override var icon: Icon = ImageIcon(this::class.java.classLoader.getResource("icons/tracker_stop_icon_16.png"))

    override val shortcut = "control shift L"
    private val myTimer = timer
    private val myRepo = repo
    private val myProject = project

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val time = myTimer.stop(myProject, taskManager.activeTask.id)

            val trackerNote = TrackerNotification()

            if (time == "0")
                trackerNote.notify("Time was not recorded (less than 1 minute)")
            else{
                val status = TimeTrackerRestClient(myRepo).postNewWorkItem(myTimer.issueId, myTimer.getRecordedTime())
                if (status != 200)
                    trackerNote.notify("Could not record time: time tracking is disabled")
                else
                    trackerNote.notify("Work timer stopped")
            }
        }
    }
}