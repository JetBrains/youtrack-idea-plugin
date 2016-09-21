package com.github.jk1.ytplugin.search.actions

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.search.model.Issue
import com.github.jk1.ytplugin.search.model.IssueTask
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * Takes selected issue from a tool window and sets it as an active Task manager task
 * todo: context switch options: branch, state change, etc
 */
class SetAsActiveTaskAction(val getSelectedIssue: () -> Issue?, val repo: YouTrackServer) : AnAction(
        "Set as active task",
        "Create task manager task from a selected issue and switch to it",
        AllIcons.Graph.Export), DumbAware {

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