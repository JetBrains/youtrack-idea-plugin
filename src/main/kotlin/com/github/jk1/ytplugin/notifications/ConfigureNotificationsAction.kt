package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware


class ConfigureNotificationsAction(private val notification: YouTrackNotification) :
        AnAction("Configure Notifications", "", AllIcons.General.Settings), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            // todo: user loginor id instad of 'me'
            val url = "${notification.repoUrl}/users/me?tab=notifications"
            ServiceManager.getService(BrowserLauncher::class.java).open(url)
        }
    }
}