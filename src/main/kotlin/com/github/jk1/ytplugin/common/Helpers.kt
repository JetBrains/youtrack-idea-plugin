package com.github.jk1.ytplugin.common

import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import javax.swing.SwingUtilities

fun sendNotification(
        title: String = "YouTrack plugin error",
        text: String?,
        type: NotificationType) = SwingUtilities.invokeLater {
    Notifications.Bus.notify(Notification("YouTrack Integration Plugin", title, text ?: "null", type))
}

val Any.logger : Logger
    get() = Logger.getInstance(this.javaClass)

fun String.runAction(){
    val action = ActionManager.getInstance().getAction(this)
    val context = DataManager.getInstance().dataContext
    val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context)
    action.actionPerformed(event)
}
