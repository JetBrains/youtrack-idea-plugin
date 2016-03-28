package com.github.jk1.ytplugin.common.components

import com.github.jk1.ytplugin.common.YouTrackPluginException
import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Thrown when no configured YouTrack repository can be found. Although we may still have an
 * active task from YouTrack open, REST endpoints cannot be used w/o YouTrack server
 * connection details provided.
 *
 * Embedded fix suggestion opens server configuration dialog provided by task management plugin.
 */
class NoYouTrackRepositoryException() :
        YouTrackPluginException("YouTrack server integration is not configured yet") {

    // defined in task management plugin we depend on
    val CONFIGURE_SERVERS_ACTION_ID = "tasks.configure.servers"

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack plugin error",
            """$message
            <br/><br/>
            <b><a href="#open">Set up YouTrack server connection</a></b>""",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, notificationEvent ->
                val action = ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID)
                val context = DataManager.getInstance().dataContext
                val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context)
                notification.hideBalloon()
                action.actionPerformed(event)
            }
    )
}