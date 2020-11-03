package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.ui.WorkItemsList
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

class ToggleGroupByAction(val repo: YouTrackServer) : IssueAction() {
    override val text = "Group spent time by issue"
    override val description = "Group spent time by issue id"
    override val icon = AllIcons.Actions.GroupBy
    override val shortcut = "control alt shift Q"


    private var GROUP_BY_DATE = false

    init {
        templatePresentation.icon = when (GROUP_BY_DATE) {
            true -> AllIcons.Actions.GroupBy
            false -> AllIcons.Actions.GroupByPrefix
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val workItemsList = WorkItemsList(repo)
            if (GROUP_BY_DATE) {
                logger.debug("Spent time grouping by date for ${repo.url}")
                workItemsList.issueWorkItemsStoreComponent[repo].withGroupingByIssue = false
                GROUP_BY_DATE = false
                event.presentation.icon = AllIcons.Actions.GroupBy
                event.presentation.text = "Group spent time by issue"
                event.presentation.description = "Group spent time by issue id"
            } else {
                logger.debug("Spent time grouping by issue for ${repo.url}")
                workItemsList.issueWorkItemsStoreComponent[repo].withGroupingByIssue = true
                GROUP_BY_DATE = true
                event.presentation.icon = AllIcons.Actions.GroupByPrefix
                event.presentation.text = "Group spent time by date"
                event.presentation.description = "Group spent time by date of recording"
            }

            ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
        }
    }

    override fun update(event: AnActionEvent) {
        event.whenActive { project ->
            event.presentation.isEnabled = !ComponentAware.of(project).issueWorkItemsStoreComponent[repo].isUpdating()
        }
    }
}