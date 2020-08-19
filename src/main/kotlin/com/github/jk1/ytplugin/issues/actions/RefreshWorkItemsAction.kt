package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Starts async issue store update from a remote server
 */
class RefreshWorkItemsAction(val repo: YouTrackServer) : IssueAction() {

    override val text = "Refresh"
    override val description = "Update work items list from YouTrack server"
    override val icon = AllIcons.Actions.Refresh
    override val shortcut = "control alt shift U"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            logger.debug("Work items refresh requested for ${repo.url}")
            ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabled = project != null &&
                project.isInitialized &&
                !ComponentAware.of(project).issueWorkItemsStoreComponent[repo].isUpdating()
    }
}