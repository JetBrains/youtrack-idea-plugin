package com.github.jk1.ytplugin.search.actions

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.tasks.Task

/**
 * Takes selected issue from a tool window and sets it as an active Task manager task
 * todo: context switch options: branch, state change, etc
 */
class SetAsActiveTaskAction(val getSelectedIssue: () -> Issue?) : AnAction(
        "Set as active task",
        "Create task manager task from a selected issue and switch to it",
        AllIcons.Graph.Export) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            val task = getSelectedIssue.invoke()?.asTask()
            if (task != null) {
                ComponentAware.of(project).taskManagerComponent.setActiveTask(task)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}