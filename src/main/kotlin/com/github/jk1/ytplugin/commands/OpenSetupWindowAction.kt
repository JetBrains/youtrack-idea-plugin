package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.setup.SetupDialog
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 *
 * Dumb aware actions can be executed when IDE is rebuilding indexes.
 */
class OpenSetupWindowAction(repo: YouTrackServer, private val fromTracker: Boolean) : AnAction(
        "Open Setup Dialog",
        "View and update plugin settings",
        AllIcons.General.Settings), DumbAware, IdeNotificationsTrait {

    val shortcut = "control shift Q"
    val repository = repo

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            SetupDialog(project, repository, fromTracker).show()
        } else {
            showError("Can't open YouTrack setup window", "No open project found")
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}