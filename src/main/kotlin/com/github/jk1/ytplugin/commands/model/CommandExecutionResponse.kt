package com.github.jk1.ytplugin.commands.model

import com.github.jk1.ytplugin.YouTrackCommandExecutionResult

class CommandExecutionResponse(
        val messages: List<String> = listOf(),
        val errors: List<String> = listOf()): YouTrackCommandExecutionResult {

    override fun isSuccessful() = errors.isEmpty()

    override fun getExecutionMessages() = messages

    override fun getExecutionErrors() = errors
}