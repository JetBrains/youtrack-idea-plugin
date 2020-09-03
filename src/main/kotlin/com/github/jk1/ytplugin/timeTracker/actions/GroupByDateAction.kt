package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.ui.WorkItemsList
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

class GroupByDateAction : IssueAction() {
    override val text = "Group work items by date"
    override val description = "Group work items by date"
    override val icon = AllIcons.Actions.GroupBy
    override val shortcut = "control alt shift Q"


    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val repo = project.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }
            val workItemsList = repo.let { WorkItemsList(it) }
            logger.debug("Work items grouping by date for ${repo.url}")
            workItemsList.issueWorkItemsStoreComponent[repo].withGrouping = false
            ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null){
            val repo = project.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }
            event.presentation.isEnabled = project.isInitialized &&
                    !ComponentAware.of(project).issueWorkItemsStoreComponent[repo].isUpdating()
        }
    }
}