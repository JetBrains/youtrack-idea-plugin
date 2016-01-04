package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.YouTrackParsedCommand
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project


class CommandRestProjectComponent(val project: Project) : AbstractProjectComponent(project), CommandProjectComponent {

    override fun execute(command: YouTrackCommand) {
        println("Executing command: ${command.command}")
    }

    override fun parse(command: YouTrackCommand) : YouTrackParsedCommand {
        throw UnsupportedOperationException()
    }
}