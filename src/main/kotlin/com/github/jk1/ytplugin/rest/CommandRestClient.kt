package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.model.CommandAssistResponse
import com.github.jk1.ytplugin.model.CommandExecutionResponse
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.jdom.input.SAXBuilder
import java.net.URLEncoder

class CommandRestClient(override val project: Project) : AbstractRestClient(project), ResponseLoggerTrait {

    override val logger: Logger = Logger.getInstance(CommandRestClient::class.java)

    fun assistCommand(command: YouTrackCommand): CommandAssistResponse {
        val method = GetMethod(command.intellisenseCommandUrl)
        val startTime = System.currentTimeMillis()
        try {
            val status = createHttpClient().executeMethod(method)
            if (status == 200) {
                return CommandAssistResponse(method.responseBodyAsLoggedStream())
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        } finally {
            method.releaseConnection()
            logger.debug("Intellisense request to YouTrack took ${System.currentTimeMillis() - startTime} ms")
        }
    }

    fun executeCommand(command: YouTrackCommand): CommandExecutionResponse {
        val method = PostMethod(command.executeCommandUrl)
        val startTime = System.currentTimeMillis()
        try {
            val status = createHttpClient().executeMethod(method)
            if (status != 200) {
                val string = method.responseBodyAsStream
                val element = SAXBuilder(false).build(string).rootElement
                if ("error".equals(element.name)) {
                    return CommandExecutionResponse(errors = listOf(element.text))
                } else {
                    return CommandExecutionResponse(messages = listOf(element.text))
                }
            }
            return CommandExecutionResponse()
        } finally {
            method.releaseConnection()
            logger.debug("Command execution request to YouTrack took ${System.currentTimeMillis() - startTime} ms")
        }
    }

    private val YouTrackCommand.executeCommandUrl: String
        get() {
            val baseUrl = taskManagerComponent.getYouTrackRepository().url
            return "$baseUrl/rest/issue/execute/${issues.first().id}?command=$encodedCommand"
        }

    private val YouTrackCommand.intellisenseCommandUrl: String
        get() {
            val baseUrl = taskManagerComponent.getYouTrackRepository().url
            return "$baseUrl/rest/command/underlineAndSuggestAndCommands?command=$encodedCommand&caret=$caret&query=${issues.first().id}"
        }

    private val YouTrackCommand.encodedCommand: String
        get() = URLEncoder.encode(command, "UTF-8")
}