package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.CommandParseResult
import com.github.jk1.ytplugin.model.YouTrackCommand


interface RestComponent {

    fun getUserGroups(login: String) : List<String>

    fun parseCommand(command: YouTrackCommand) : CommandParseResult

    fun executeCommand(command: YouTrackCommand) : CommandExecutionResult
}