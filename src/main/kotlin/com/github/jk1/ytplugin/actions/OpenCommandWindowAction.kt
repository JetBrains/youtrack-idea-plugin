package com.github.jk1.ytplugin.actions

import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.sendNotification
import com.github.jk1.ytplugin.view.CommandDialog
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class OpenCommandWindowAction : AnAction(
        "Execute YouTrack command",
        "Apply YouTrack command to a current active task",
        AllIcons.Actions.Execute) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            try {
                CommandDialog(project).show()
            } catch(e: YouTrackPluginException) {
                sendNotification("Can't open YouTrack command window", e.message, NotificationType.ERROR)
                e.printStackTrace()
            }
        } else {
            //todo: log it
        }
    }

    override fun update(event: AnActionEvent) {

    }
}