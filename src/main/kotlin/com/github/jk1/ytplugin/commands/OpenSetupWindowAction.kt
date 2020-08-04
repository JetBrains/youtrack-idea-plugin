package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.setupWindow.SetupDialog
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 *
 * Dumb aware actions can be executed when IDE is rebuilding indexes.
 */
class OpenSetupWindowAction(val repository: YouTrackServer) : IssueAction(), DumbAware, IdeNotificationsTrait {

    override val text = "Open Setup Dialog"
    override val description = "Open configuration settings"
    override val icon = AllIcons.General.Settings
    override val shortcut = "control shift Q"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
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
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}
