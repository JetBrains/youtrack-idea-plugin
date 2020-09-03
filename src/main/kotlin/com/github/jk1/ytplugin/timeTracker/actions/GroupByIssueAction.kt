package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.ui.WorkItemsList
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class GroupByIssueAction: AnAction(
        "Group spent time by issue",
        "Group spent time by issue",
        AllIcons.Actions.GroupByPrefix), DumbAware, IdeNotificationsTrait {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val repo = project.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }
            val workItemsList = repo.let { WorkItemsList(it) }
            logger.debug("Spent time grouping by issue for ${repo.url}")
            workItemsList.issueWorkItemsStoreComponent[repo].withGrouping = true
            ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val repo = project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }

        event.presentation.isEnabled = project != null && repo != null &&
                project.isInitialized &&
                !ComponentAware.of(project).issueWorkItemsStoreComponent[repo].isUpdating()
    }
}