package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.timeTracker.AllSavedTimerItemsDialog
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ShowAllSavedTimeTrackingItems : AnAction(
    "Show Tracked Time",
    "Show local time tracking records that have yet to be posted to YouTrack",
     AllIcons.Vcs.History // TODO custom icon
) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        // to be able to view time tracking items even when 'None' tracking mode is selected
        event.whenActive {
            val repos =
                project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getAllConfiguredYouTrackRepositories() }
            val repo = repos?.first()
            project?.let { pr -> repo?.let { repository -> AllSavedTimerItemsDialog(pr, repository).show() } }
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            try {
                // display action when YouTrack repo is configured
                event.presentation.isVisible = ComponentAware.of(project).taskManagerComponent
                    .getAllConfiguredYouTrackRepositories().isNotEmpty()
            } catch (e: NoYouTrackRepositoryException) {
                event.presentation.isVisible = false
            }
        }
    }
}


