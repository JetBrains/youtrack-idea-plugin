package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.common.YouTrackPluginException
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.sendNotification
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 *
 */
class OpenCommandWindowAction : AnAction(
        "Execute YouTrack command",
        "Apply YouTrack command to a current active task",
        AllIcons.Actions.Execute) {

    val errorTitle = "Can't open YouTrack command window"

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            try {
                assertYouTrackRepositoryConfigured(project)
                CommandDialog(project).show()
            } catch(exception: YouTrackPluginException) {
                exception.showAsNotificationBalloon(project)
            }
        } else {
            sendNotification(errorTitle, "No open project found", NotificationType.ERROR)
        }
    }

    private fun assertYouTrackRepositoryConfigured(project: Project){
        ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}