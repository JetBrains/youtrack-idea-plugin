package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandAssistResponse
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.rest.CommandRestClient
import com.github.jk1.ytplugin.sendNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.util.containers.hash.LinkedHashMap
import javax.swing.SwingUtilities


class CommandComponentImpl(override val project: Project) :
        AbstractProjectComponent(project), CommandComponent, ComponentAware {

    val restClient = CommandRestClient(project)

    override fun executeAsync(command: YouTrackCommand) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                command.issues.add(taskManagerComponent.getActiveTask())
                val result = restClient.executeCommand(command)
                SwingUtilities.invokeLater {
                    result.errors.forEach {
                        sendNotification("Command execution error", it, NotificationType.ERROR)
                    }
                    result.messages.forEach {
                        sendNotification("YouTrack server message", it, NotificationType.INFORMATION)
                    }
                }
            } catch(e: Throwable) {
                //todo: redirect to event log
                e.printStackTrace()
            }
        }
    }

    override fun suggest(command: YouTrackCommand): CommandAssistResponse {
        command.issues.add(taskManagerComponent.getActiveTask())
        val response = IntellisenseCache[command] ?: restClient.assistCommand(command)
        IntellisenseCache[command] = response
        return response
    }

    private object IntellisenseCache : LinkedHashMap<YouTrackCommand, CommandAssistResponse>(10, true) {
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