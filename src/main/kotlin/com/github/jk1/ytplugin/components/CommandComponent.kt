package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.CommandParseResult
import com.intellij.openapi.components.ProjectComponent


interface CommandComponent : ProjectComponent {

    fun execute(command: YouTrackCommand): CommandExecutionResult
    fun parse(command: YouTrackCommand): CommandParseResult
}