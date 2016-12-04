package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.runAction
import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType

/**
 * Thrown when no configured YouTrack repository can be found. Although we may still have an
 * active task from YouTrack open, REST endpoints cannot be used w/o YouTrack server
 * connection details provided.
 *
 * Embedded fix suggestion opens server configuration dialog provided by task management plugin.
 */
class NoYouTrackRepositoryException() :
        YouTrackPluginException("YouTrack server integration is not configured yet") {

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack plugin error",
            """$message
            <br/>
            <b><a href="#open">Set up YouTrack server connection</a></b>""",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, notificationEvent ->
                notification.hideBalloon()
                CONFIGURE_SERVERS_ACTION_ID.runAction()
            }
    )
}