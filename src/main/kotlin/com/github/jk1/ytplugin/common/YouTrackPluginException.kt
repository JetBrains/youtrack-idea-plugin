package com.github.jk1.ytplugin.common

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import javax.swing.SwingUtilities


abstract class YouTrackPluginException(override val message: String) : Exception(message) {

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

//class NoYouTrackRepositoryException() :
//        YouTrackPluginException("No YouTrack server found") {}

//class YouTrackRepositoryNotConfiguredException() :
//        YouTrackPluginException("YouTrack server integration is not configured yet") {}


