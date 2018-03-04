package com.github.jk1.ytplugin.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import javax.swing.SwingUtilities

interface IdeNotificationsTrait
{
    fun showErrorNotification(
            title: String = "YouTrack plugin error",
            text: String?,
            type: NotificationType) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(Notification("YouTrack Integration Plugin", title, text ?: "null", type))
    }
}