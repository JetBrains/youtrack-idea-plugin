package com.github.jk1.ytplugin.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import javax.swing.SwingUtilities

interface IdeNotificationsTrait {

    private val groupId get() = "YouTrack Integration Plugin"

    fun showNotification(title: String, text: String) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(Notification(groupId, title, text, NotificationType.INFORMATION))
    }

    fun showWarning(title: String = "YouTrack plugin error", text: String) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(Notification(groupId, title, text, NotificationType.WARNING))
    }

    fun showError(title: String = "YouTrack plugin error", text: String) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(Notification(groupId, title, text, NotificationType.ERROR))
    }
}