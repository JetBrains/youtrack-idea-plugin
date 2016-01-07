package com.github.jk1.ytplugin.model

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType

class CommandExecutionResult(val messages: List<String> = listOf(), val errors: List<String> = listOf()) {

    companion object {
        val notificationGroupId = "YouTrack Command Executor"
        val messageTitle = "YouTrack Command Applied"
        val errorTitle = "YouTrack Command Failed"
    }

    fun isSuccessful() = errors.isEmpty()

    fun hasMessages() = messages.isNotEmpty()

    fun hasErrors() = errors.isNotEmpty()

    val notifications: Iterable<Notification>
        get() {
            return errors.map {
                Notification(notificationGroupId, errorTitle, it, NotificationType.ERROR)
            }.union(messages.map {
                Notification(notificationGroupId, messageTitle, it, NotificationType.INFORMATION)
            })
        }
}