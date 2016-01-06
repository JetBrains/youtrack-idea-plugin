package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.CommandParseResult


interface RestComponent {

    fun parseCommand(command: YouTrackCommand) : CommandParseResult

    fun executeCommand(command: YouTrackCommand) : CommandExecutionResult
}