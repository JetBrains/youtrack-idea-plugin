package com.github.jk1.ytplugin.search.actions

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.tasks.Task


class SetAsActiveTaskAction(val getSelectedTask: () -> Task) : AnAction(
        "Set as active task",
        "Create task manager task from a selected issue and switch to it",
        AllIcons.Graph.Export) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            val task = getSelectedTask.invoke()
            ComponentAware.of(project).taskManagerComponent.setActiveTask(task)
        }
    }
}