package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.setupWindow.SetupDialog
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import javax.swing.Icon
import javax.swing.JComponent

/**
 *
 * Dumb aware actions can be executed when IDE is rebuilding indexes.
 */
class OpenSetupWindowAction : AnAction(
        "Open Setup Dialog",
        "Open configuration settings",
        AllIcons.General.Settings), DumbAware, IdeNotificationsTrait {

    val shortcut = "control shift Q"

    fun register(parent: JComponent) {
        registerCustomShortcutSet(CustomShortcutSet.fromString(shortcut), parent)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            try {
                SetupDialog(project).show()

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