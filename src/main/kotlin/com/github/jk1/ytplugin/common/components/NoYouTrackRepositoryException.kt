package com.github.jk1.ytplugin.common.components

import com.github.jk1.ytplugin.common.YouTrackPluginException
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType


class NoYouTrackRepositoryException():
        YouTrackPluginException("YouTrack server integration is not configured yet") {

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack plugin error",
            "$message <br/><br/><b><a href=\"#open\">Set up YouTrack server connection</a></b>",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, event ->

                notification.hideBalloon()
            }
    )
}