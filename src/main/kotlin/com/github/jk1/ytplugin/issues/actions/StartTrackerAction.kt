package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.tasks.TaskManager

class StartTrackerAction(repo: YouTrackServer, timer: TimeTracker, taskManager: TaskManager) : IssueAction() {
    override val text = "Start time tracking"
    override val description = "Start time tracking"
    override var icon = AllIcons.Actions.Profile
    override val shortcut = "control shift K"
    private var myManager = taskManager
    private val myTimer = timer
    private val myRepo = repo

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val activeTask = myManager.activeTask
            myTimer.issueId =  IssuesRestClient(myRepo).getEntityIdByIssueId(activeTask.id)
            myTimer.start()
        }
    }
}