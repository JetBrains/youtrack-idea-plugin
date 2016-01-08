package com.github.jk1.ytplugin.model

class CommandExecutionResult(val messages: List<String> = listOf(), val errors: List<String> = listOf()) {

    fun isSuccessful() = errors.isEmpty()

    fun hasMessages() = messages.isNotEmpty()

    fun hasErrors() = errors.isNotEmpty()
}