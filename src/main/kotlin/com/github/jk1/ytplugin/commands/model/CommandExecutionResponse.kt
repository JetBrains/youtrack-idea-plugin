package com.github.jk1.ytplugin.commands.model

class CommandExecutionResponse(val messages: List<String> = listOf(), val errors: List<String> = listOf()) {

    fun isSuccessful() = errors.isEmpty()

    fun hasMessages() = messages.isNotEmpty()

    fun hasErrors() = errors.isNotEmpty()
}