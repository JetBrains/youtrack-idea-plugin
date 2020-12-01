package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import java.awt.Robot
import java.awt.event.KeyEvent


class TrackerNotification {

    private val NOTIFICATION_GROUP = NotificationGroup("Time tracking notifications", NotificationDisplayType.BALLOON, true)

    fun notify(content: String, type: NotificationType): Notification {
        return notify(null, content, type, null)
    }

    fun notifyWithHelper(content: String, type: NotificationType, helper: AnAction): Notification {
        return notify(null, content, type, helper)
    }

    fun notify(project: Project?, content: String, type: NotificationType, action: AnAction?): Notification {
        val notification: Notification = NOTIFICATION_GROUP.createNotification(content, type)
        if (action != null){
            notification.addAction(action)
        }
        notification.notify(project)
        return notification
    }
}

class OpenActiveTaskSelection :
        AnAction("Select task"), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val robot = Robot()
            robot.keyPress(KeyEvent.VK_SHIFT)
            robot.keyPress(KeyEvent.VK_ALT)
            robot.keyPress(KeyEvent.VK_T)

            robot.keyRelease(KeyEvent.VK_SHIFT)
            robot.keyRelease(KeyEvent.VK_ALT)
            robot.keyRelease(KeyEvent.VK_T)
        }
    }
}