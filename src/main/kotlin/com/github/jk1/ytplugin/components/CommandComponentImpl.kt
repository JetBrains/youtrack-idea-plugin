package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.CommandParseResult
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project


class CommandComponentImpl(override val project: Project) :
        AbstractProjectComponent(project), CommandComponent, ComponentAware {

    override fun execute(command: YouTrackCommand) : CommandExecutionResult {
        val task = taskManagerComponent.getActiveTask()
        command.issues.add(task)
        return restComponent.executeCommand(command)
    }

    override fun parse(command: YouTrackCommand): CommandParseResult {
        val task = taskManagerComponent.getActiveTask()
        command.issues.add(task)
        return restComponent.parseCommand(command)
    }


}