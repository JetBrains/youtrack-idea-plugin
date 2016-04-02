package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.commands.rest.CommandRestClient
import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.common.sendNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.containers.hash.LinkedHashMap


class CommandComponentImpl(override val project: Project) : AbstractProjectComponent(project), CommandComponent {

    val restClient = CommandRestClient(project)
    val assistCache = CommandSuggestResponseCache(project)

    override fun executeAsync(execution: YouTrackCommandExecution) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                execution.command.issues.add(taskManagerComponent.getActiveTask())
                val result = restClient.executeCommand(execution)
                result.errors.forEach {
                    sendNotification("Command execution error", it, NotificationType.ERROR)
                }
                result.messages.forEach {
                    sendNotification("YouTrack server message", it, NotificationType.INFORMATION)
                }
            } catch(e: Throwable) {
                sendNotification("Command execution error", e.message, NotificationType.ERROR)
                logger.error("Command execution error", e)
            }
        }
    }

    override fun suggest(command: YouTrackCommand): CommandAssistResponse {
        command.issues.add(taskManagerComponent.getActiveTask())
        val response = assistCache[command] ?: restClient.assistCommand(command)
        assistCache[command] = response
        return response
    }
}