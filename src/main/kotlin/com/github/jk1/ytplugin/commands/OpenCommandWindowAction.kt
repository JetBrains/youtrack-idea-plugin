package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.IssueTask
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.ui.CommandDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 *
 * Dumb aware actions can be executed when IDE is rebuilding indexes.
 */
class OpenCommandWindowAction : AnAction(
        "Open Command Dialog",
        "Apply YouTrack command to a current active task",
        AllIcons.Debugger.Console), DumbAware, IdeNotificationsTrait {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            try {
                assertYouTrackRepositoryConfigured(project)
                val issue = getIssueFromCurrentActiveTask(project)
                CommandDialog(project, issue).show()
            } catch (exception: YouTrackPluginException) {
                exception.showAsNotificationBalloon(project)
            }
        } else {
            showError("Can't open YouTrack command window", "No open project found")
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    private fun assertYouTrackRepositoryConfigured(project: Project) {
        val repos = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        if (repos.isEmpty()) {
            throw NoYouTrackRepositoryException()
        }
    }

    private fun getIssueFromCurrentActiveTask(project: Project): Issue {
        return ComponentAware.of(project) {
            val task = taskManagerComponent.getActiveYouTrackTask()
            if (task is IssueTask) {
                task.issue
            } else {
                // try local store first, fall back to rest api if not found
                try {
                    val repo = taskManagerComponent.getActiveYouTrackRepository()
                    issueStoreComponent[repo].firstOrNull { it.id == task.id }
                            ?: IssuesRestClient(repo).getIssue(task.id)
                            ?: throw NoActiveYouTrackTaskException()
                } catch (e: NoYouTrackRepositoryException) {
                    logger.debug("Unable to get issue from current activeTask: ${e.message} ")
                    throw NoActiveYouTrackTaskException()
                }

            }
        }
    }
}