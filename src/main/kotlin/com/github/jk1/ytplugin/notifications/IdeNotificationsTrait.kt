package com.github.jk1.ytplugin.notifications

import com.intellij.notification.*
import javax.swing.SwingUtilities

interface IdeNotificationsTrait {

    private val groupId get() = "YouTrack Integration Plugin"

    fun showNotification(title: String, text: String) = SwingUtilities.invokeLater {
        val group = NotificationGroup(groupId, NotificationDisplayType.STICKY_BALLOON, false)
        group.createNotification(title, text, NotificationType.INFORMATION, null).notify(null)
    }

    fun showWarning(title: String = "YouTrack plugin error", text: String) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(Notification(groupId, title, text, NotificationType.WARNING))
    }

    fun showError(title: String = "YouTrack plugin error", text: String) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(Notification(groupId, title, text, NotificationType.ERROR))
    }
}