package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Starts async issue store update from a remote server
 */
class RefreshWorkItemsAction : IssueAction() {

    override val text = "Refresh spent time"
    override val description = "Update spent time list from YouTrack server"
    override val icon = AllIcons.Actions.Refresh
    override val shortcut = "control alt shift U"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val repo = project.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }
            logger.debug("Spent time refresh requested for ${repo.url}")
            ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val repo = project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }

        event.presentation.isEnabled = project != null &&
                project.isInitialized && repo != null &&
                !ComponentAware.of(project).issueWorkItemsStoreComponent[repo].isUpdating()
    }
}