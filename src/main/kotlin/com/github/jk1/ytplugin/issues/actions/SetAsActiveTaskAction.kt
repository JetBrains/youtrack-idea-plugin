package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Takes selected issue from a tool window and sets it as an active Task manager task
 * todo: context switch options: branch, state change, etc
 */
class SetAsActiveTaskAction(private val getSelectedIssue: () -> Issue?, val repo: YouTrackServer) : IssueAction() {

    override val text = "Open Task"
    override val description = "Create task manager task from a selected issue and switch to it"
    override val icon = AllIcons.Graph.Export!!
    override val shortcut = "control shift A"

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            val issue = getSelectedIssue.invoke()
            if (issue != null) {
                with(ComponentAware.of(project)) {
                    taskManagerComponent.setActiveTask(repo.createTask(issue))
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}