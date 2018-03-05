package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware


class BrowseNotificationAction(private val notification: YouTrackNotification) :
        AnAction("Browse", "Navigate to ", AllIcons.General.Web), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            ServiceManager.getService(BrowserLauncher::class.java).open(notification.url)
        }
    }
}