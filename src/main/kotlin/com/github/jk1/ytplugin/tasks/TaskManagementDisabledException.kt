package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.YouTrackPluginException
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.extensions.PluginId

/**
 * Thrown if no task management plugin is found. We rely heavily on this plugin to provide us
 * with server connection details and overall IDE context management. Quickfix suggests a user
 * to enable plugin and resolve the problem.
 */
class TaskManagementDisabledException :
        YouTrackPluginException(
                "Task Management plugin is disabled. This plugin is required for YouTrack to work properly") {

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack plugin error",
            """$message
            <br/>
            <b><a href="#open">Enable Plugin</a></b>""",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, _ ->
                notification.hideBalloon()
                PluginManager.enablePlugin("com.intellij.tasks")
            }
    )
}