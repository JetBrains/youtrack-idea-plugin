package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.CommandRestClient
import com.github.jk1.ytplugin.sendNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import java.util.concurrent.Future


class CommandComponentImpl(override val project: Project) : AbstractProjectComponent(project), CommandComponent {

    val restClient = CommandRestClient(project)
    private val assistCache = CommandSuggestResponseCache(project)

    override fun executeAsync(execution: YouTrackCommandExecution): Future<Unit> {
        val future = FutureResult<Unit>()
        object : Task.Backgroundable(project, "Executing YouTrack command") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = title
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
                } finally {
                    future.set(Unit)
                }
            }
        }.queue()
        return future
    }

    override fun suggest(command: YouTrackCommand): CommandAssistResponse {
        val response = assistCache[command] ?: restClient.assistCommand(command)
        assistCache[command] = response
        return response
    }
}