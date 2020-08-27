package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.ui.WorkItemsList
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

class GroupByIssueAction(val repo: YouTrackServer, val workItemsList: WorkItemsList) : IssueAction() {
    override val text = "Group work items by issue"
    override val description = "Group work items by issue"
    override val icon = AllIcons.Actions.GroupByPrefix
    override val shortcut = "control alt shift W"


    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            logger.debug("Work items grouping by issue for ${repo.url}")
            workItemsList.issueWorkItemsStoreComponent[repo].withGrouping = true
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