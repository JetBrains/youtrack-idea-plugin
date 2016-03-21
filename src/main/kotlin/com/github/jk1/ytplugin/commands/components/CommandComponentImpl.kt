package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.commands.rest.CommandRestClient
import com.github.jk1.ytplugin.common.sendNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.containers.hash.LinkedHashMap


class CommandComponentImpl(override val project: Project) : AbstractProjectComponent(project), CommandComponent {

    val logger: Logger = Logger.getInstance(CommandComponentImpl::class.java)
    val restClient = CommandRestClient(project)

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
        val response = CommandAssistCache[command] ?: restClient.assistCommand(command)
        CommandAssistCache[command] = response
        return response
    }

    private object CommandAssistCache : LinkedHashMap<YouTrackCommand, CommandAssistResponse>(10, true) {
        override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<YouTrackCommand, CommandAssistResponse>,
                key: YouTrackCommand, value: CommandAssistResponse):
                Boolean = this.size > 30

        override fun get(key: YouTrackCommand?): CommandAssistResponse? {
            synchronized(this) {
                return super.get(key)
            }
        }

        override fun put(key: YouTrackCommand?, value: CommandAssistResponse): CommandAssistResponse? {
            synchronized(this) {
                return super.put(key, value)
            }
        }
    }
}