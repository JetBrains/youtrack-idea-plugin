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
import java.util.*
import java.util.concurrent.TimeUnit


class CommandComponentImpl(override val project: Project) : AbstractProjectComponent(project), CommandComponent {

    val logger: Logger = Logger.getInstance(CommandComponentImpl::class.java)
    val restClient = CommandRestClient(project)
    val cache = SuggestResponseCache()

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
        val response = cache[command] ?: restClient.assistCommand(command)
        cache[command] = response
        return response
    }

    inner class SuggestResponseCache {

        val caches = HashMap<String, PerServerSuggestResponseCache>()

        operator fun get(key: YouTrackCommand?): CommandAssistResponse? {
            synchronized(this) {
                return getCache()[key]
            }
        }

        operator fun set(key: YouTrackCommand?, value: CommandAssistResponse) {
            synchronized(this) {
                getCache().put(key, value)
            }
        }

        private fun getCache(): PerServerSuggestResponseCache{
            val server = taskManagerComponent.getActiveYouTrackRepository().url
            if (!caches.containsKey(server)){
                caches.putIfAbsent(server, PerServerSuggestResponseCache())
            }
            return caches[server]!!
        }
    }

    inner class PerServerSuggestResponseCache : LinkedHashMap<YouTrackCommand, CommandAssistResponse>(10, true) {

        val CACHE_ENTRY_TTL = TimeUnit.MINUTES.convert(30, TimeUnit.MILLISECONDS)

        override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<YouTrackCommand, CommandAssistResponse>,
                key: YouTrackCommand, value: CommandAssistResponse):
                Boolean = this.size > 30

        override fun get(key: YouTrackCommand?): CommandAssistResponse? {
            super.get(key)?.let {
                if (Math.abs(System.currentTimeMillis() - it.timestamp) > CACHE_ENTRY_TTL) {
                    remove(key)
                }
            }
            return super.get(key)
        }
    }
}