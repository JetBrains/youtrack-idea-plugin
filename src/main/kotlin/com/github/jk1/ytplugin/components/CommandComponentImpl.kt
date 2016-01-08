package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandParseResult
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

    override fun execute(command: YouTrackCommand) {
        executor.submit {
            try {
                command.issues.add(taskManagerComponent.getActiveTask())
                val result = restClient.executeCommand(command)
                SwingUtilities.invokeLater {
                    result.errors.forEach { sendNotification(text = it, type = NotificationType.ERROR) }
                    result.messages.forEach { sendNotification(text = it, type = NotificationType.INFORMATION)  }
                }
            } catch(e: Throwable) {
                //todo: redirect to event log
                e.printStackTrace()
            }
        }
    }

    override fun parse(command: YouTrackCommand): CommandParseResult {
        val task = taskManagerComponent.getActiveTask()
        command.issues.add(task)
        return restClient.parseCommand(command)
    }
}