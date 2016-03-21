package com.github.jk1.ytplugin.common

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import javax.swing.SwingUtilities

fun sendNotification(
        title: String = "YouTrack plugin error",
        text: String?,
        type: NotificationType) = SwingUtilities.invokeLater {
    Notifications.Bus.notify(Notification("YouTrack Integration Plugin", title, text ?: "null", type))
}

val Any.logger : Logger
    get() = Logger.getInstance(this.javaClass)
