package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.YouTrackPluginException
import com.github.jk1.ytplugin.runAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType

/**
 * Indicates that no active task is currently selected, or a selected task cannot be interpreted
 * as a valid YouTrack issue. A quickfix would be to invoke a "Go to task" action from the task
 * management plugin to let user pick a YouTrack issue to work on.
 */
class NoActiveYouTrackTaskException : YouTrackPluginException("No YouTrack issue selected as an active task") {

    // defined in task management plugin we depend on
    val GOTO_TASK_ACTION_ID = "tasks.goto"

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack Integration Plugin",
            """$message
            <br/>
            <b><a href="#open">Select Issue</a></b>""",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, notificationEvent ->
                notification.hideBalloon()
                GOTO_TASK_ACTION_ID.runAction()
            }
    )
}