package com.github.jk1.ytplugin.common.components

import com.github.jk1.ytplugin.common.YouTrackPluginException
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType

class TaskManagementDisabledException() :
        YouTrackPluginException(
                "Task Management plugin is disabled. This plugin is required for YouTrack to work properly") {

    override val notification = Notification(
            "YouTrack Integration Plugin",
            "YouTrack plugin error",
            "$message <br/><br/><b><a href=\"#open\">Enable Plugin</a></b>",
            NotificationType.ERROR,
            // notification hyperlink click handler
            NotificationListener { notification, event ->
                PluginManager.enablePlugin("com.intellij.tasks")
                notification.hideBalloon()
            }
    )
}