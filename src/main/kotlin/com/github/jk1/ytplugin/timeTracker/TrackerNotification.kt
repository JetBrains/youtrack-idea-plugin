package com.github.jk1.ytplugin.timeTracker

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project


class TrackerNotification {
    private val NOTIFICATION_GROUP = NotificationGroup("Time tracking notifications", NotificationDisplayType.BALLOON, true)

    fun notify(content: String?): Notification {
        return notify(null, content)
    }

    fun notify(project: Project?, content: String?): Notification {
        val notification: Notification = NOTIFICATION_GROUP.createNotification(content!!, NotificationType.INFORMATION)
        notification.notify(project)
        return notification
    }
}