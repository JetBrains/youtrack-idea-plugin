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
 * Indicates that no active task is currently selected, or a selected task cannot be interpreted
 * as a valid YouTrack issue. A quickfix would be to invoke a "Go to task" action from the task
 * management plugin to let user pick a YouTrack issue to work on.
 */
class NoActiveYouTrackTaskException : YouTrackPluginException("No YouTrack issue selected as an active task") {

    // defined in task management plugin we depend on
    val GOTO_ACTION_TASK_ID = "tasks.goto"

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack Integration Plugin",
            """$message
            <br/><br/>
            <b><a href="#open">Select Issue</a></b>""",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, notificationEvent ->
                val action = ActionManager.getInstance().getAction(GOTO_ACTION_TASK_ID)
                val context = DataManager.getInstance().dataContext
                val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context)
                notification.hideBalloon()
                action.actionPerformed(event)
            }
    )
}