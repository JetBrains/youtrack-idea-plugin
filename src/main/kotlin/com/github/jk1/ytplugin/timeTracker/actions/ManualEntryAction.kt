package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.timeTracker.TimeTrackerManualEntryDialog
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ManualEntryAction : AnAction(
        "Add Spent Time",
        "Post a new work item to your YouTrack server",
        YouTrackPluginIcons.YOUTRACK_MANUAL_ADD_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val repo = project.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }
            val dialog = repo.let { it1 -> TimeTrackerManualEntryDialog(project, it1) }
            dialog.show()
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            try {
                // to avoid displaying control only when repo is configured and valid Active task is selected.
                // display it when YouTrack repo is just configured
                event.presentation.isVisible = ComponentAware.of(project).taskManagerComponent
                    .getAllConfiguredYouTrackRepositories().isNotEmpty()
            } catch (e: NoYouTrackRepositoryException) {
                event.presentation.isVisible = false
            }
        }
    }
}