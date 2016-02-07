package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandAssistResponse
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.rest.CommandRestClient
import com.github.jk1.ytplugin.sendNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.util.ConcurrencyUtil
import javax.swing.SwingUtilities


class CommandComponentImpl(override val project: Project) :
        AbstractProjectComponent(project), CommandComponent, ComponentAware {

    companion object {
        val executor = ConcurrencyUtil.newSingleThreadExecutor("YouTrack command executor")
    }

    val restClient = CommandRestClient(project)

    override fun executeAsync(command: YouTrackCommand) {
        executor.submit {
            try {
                command.issues.add(taskManagerComponent.getActiveTask())
                val result = restClient.executeCommand(command)
                SwingUtilities.invokeLater {
                    result.errors.forEach { sendNotification("Command execution error", it, NotificationType.ERROR) }
                    result.messages.forEach { sendNotification("YouTrack server message", it, NotificationType.INFORMATION) }
                }
            } catch(e: Throwable) {
                //todo: redirect to event log
                e.printStackTrace()
            }
        }
    }

    override fun suggest(command: YouTrackCommand): CommandAssistResponse {
        command.issues.add(taskManagerComponent.getActiveTask())
        val response = restClient.assistCommand(command)
        //IntellisenseCache.put(lookup, response)
        return response
    }

    private class Query(val command: String, val caret: Int)

/*    private object IntellisenseCache : LinkedHashMap<Query, YouTrackIntellisense.Response>(40, true) {
        override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Query, YouTrackIntellisense.Response>,
                key: Query, value: YouTrackIntellisense.Response):
                Boolean = this.size > 30

    }*/
}