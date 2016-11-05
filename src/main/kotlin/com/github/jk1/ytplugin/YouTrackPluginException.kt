package com.github.jk1.ytplugin

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import javax.swing.SwingUtilities

abstract class YouTrackPluginException(final override val message: String) : Exception(message) {

    open val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack plugin error",
            message,
            NotificationType.ERROR
    )

    fun showAsNotificationBalloon(project: Project? = null) = SwingUtilities.invokeLater {
        Notifications.Bus.notify(notification, project)
    }
}


