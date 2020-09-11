package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.setup.SetupDialog
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 *
 * Dumb aware actions can be executed when IDE is rebuilding indexes.
 */
class OpenSetupWindowAction(repo: YouTrackServer) : AnAction(
        "Open Setup Dialog",
        "Open configuration settings",
        AllIcons.General.Settings), DumbAware, IdeNotificationsTrait {

    val shortcut = "control shift Q"
    val repository = repo

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            try {
                SetupDialog(project, repository).show()
            } catch (exception: YouTrackPluginException) {
                exception.showAsNotificationBalloon(project)
            }
        } else {
            showError("Can't open YouTrack setup window", "No open project found")
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}