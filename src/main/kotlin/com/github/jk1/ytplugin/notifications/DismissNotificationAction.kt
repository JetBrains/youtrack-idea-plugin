package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware


class DismissNotificationAction(private val notification: Notification) :
        AnAction("Dismiss", "Close notification popup", AllIcons.Actions.Cancel), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            notification.expire()
        }
    }
}