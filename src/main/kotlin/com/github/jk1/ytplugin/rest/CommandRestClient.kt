package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.CommandParseResult
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.openapi.project.Project
import java.net.URLEncoder

class CommandRestClient(override val project: Project) : AbstractRestClient(project) {

    fun parseCommand(command: YouTrackCommand): CommandParseResult {
        throw UnsupportedOperationException("Parse command")
    }

    fun executeCommand(command: YouTrackCommand): CommandExecutionResult {
        doRest(command.executeCommandUrl, createHttpClient())
        return CommandExecutionResult() // todo: parse response & fill result
    }

    private val YouTrackCommand.encodedCommand: String
        get() = URLEncoder.encode(command, "UTF-8")

    private val YouTrackCommand.executeCommandUrl: String
        get() {
            val baseUrl = taskManagerComponent.getYouTrackRepository().url
            return "$baseUrl/rest/issue/execute/${issues.first().id}?command=$encodedCommand"
        }

    private val YouTrackCommand.intellisenseCommandUrl: String
        get() {
            val baseUrl = taskManagerComponent.getYouTrackRepository().url
            return "$baseUrl/rest/issue/execute/intellisense/${issues.first().id}?command=$encodedCommand"
        }
}