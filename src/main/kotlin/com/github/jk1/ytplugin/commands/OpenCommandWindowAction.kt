package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.sendNotification
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.ui.CommandDialog
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 *
 * Dumb aware actions can be executed when IDE is rebuilding indexes.
 */
class OpenCommandWindowAction : AnAction(
        "Execute YouTrack command",
        "Apply YouTrack command to a current active task",
        AllIcons.Debugger.CommandLine), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            try {
                assertYouTrackRepositoryConfigured(project)
                CommandDialog(project, CommandSession(project)).show()
            } catch(exception: YouTrackPluginException) {
                exception.showAsNotificationBalloon(project)
            }
        } else {
            sendNotification(
                    "Can't open YouTrack command window",
                    "No open project found", NotificationType.ERROR)
        }
    }

    private fun assertYouTrackRepositoryConfigured(project: Project) {
        val repos = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        if (repos.isEmpty()) {
            throw NoYouTrackRepositoryException()
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}