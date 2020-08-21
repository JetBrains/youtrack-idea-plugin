package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotifier
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager

class StartTrackerAction(repo: YouTrackServer, timer: TimeTracker, project: Project, taskManager: TaskManager) : IssueAction() {
    override val text = "Start work timer"
    override val description = "Start work timer"
    override var icon = AllIcons.Actions.Profile
    override val shortcut = "control shift K"
    private var myManager = taskManager
    private val myTimer = timer
    private val myRepo = repo
    private val myProject = project


    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val activeTask = myManager.activeTask
            if (!myTimer.isRunning) {
                myTimer.issueId = IssuesRestClient(myRepo).getEntityIdByIssueId(activeTask.id)
                if (myTimer.issueId != "0")
                    myTimer.start(myProject)
            } else
                TrackerNotifier.infoBox("Work timer is already running", "");

        }
    }
}