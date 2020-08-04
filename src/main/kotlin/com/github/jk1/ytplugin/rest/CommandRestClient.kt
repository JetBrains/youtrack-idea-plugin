package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.jdom.input.SAXBuilder
import java.io.InputStreamReader

class CommandRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun assistCommand(command: YouTrackCommand): CommandAssistResponse {
        val method = GetMethod(command.intellisenseCommandUrl)
        return method.connect {
            it.addRequestHeader("Accept", "application/json")
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                CommandAssistResponse(method.responseBodyAsLoggedStream())
            } else {
                throw RuntimeException("HTTP $status: ${method.responseBodyAsLoggedString()}")
            }
        }
    }

    fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {
        val method = PostMethod(command.executeCommandUrl)
        return method.connect {
            it.addRequestHeader("Accept", "application/json")
            val status = httpClient.executeMethod(method)
            if (status != 200) {
                val body = method.responseBodyAsLoggedStream()
                when (method.getResponseHeader("Content-Type")?.value?.split(";")?.first()) {
                    "application/xml" -> {
                        val element = SAXBuilder(false).build(body).rootElement
                        if ("error" == element.name) {
                            CommandExecutionResponse(errors = listOf(element.text))
                        } else {
                            CommandExecutionResponse(messages = listOf(element.text))
                        }
                    }
                    "application/json" -> {
                        val streamReader = InputStreamReader(body, "UTF-8")
                        val error = JsonParser.parseReader(streamReader).asJsonObject.get("value").asString
                        CommandExecutionResponse(errors = listOf("Workflow: $error"))
                    }
                    else ->
                        CommandExecutionResponse(errors = listOf("Unexpected command response from YouTrack server"))
                }
            } else {
                method.responseBodyAsLoggedString()
                CommandExecutionResponse()
            }
        }
    }

    private val YouTrackCommandExecution.executeCommandUrl: String
        get () {
            val execUrl = "${repository.url}/rest/issue/execute/${session.issue.id}"
            var params = "command=${command.urlencoded}&comment=${comment?.urlencoded}&disableNotifications=$silent"
            if (commentVisibleGroup != "All Users") {
                // 'All Users' shouldn't be passed as a parameter value. Localized YouTracks can't understand that.
                params = "$params&group=${commentVisibleGroup.urlencoded}"
            }
            return "$execUrl?$params"
        }

    private val YouTrackCommand.intellisenseCommandUrl: String
        get () {
//            val assistUrl = "${repository.url}/rest/command/underlineAndSuggestAndCommands"
            val assistUrl = "${repository.url}/api/command/underlineAndSuggestAndCommands"

            val result = "$assistUrl?command=${command.urlencoded}&caret=$caret&noIssuesContext=false"
            return if (session.hasEntityId()) {
                "$result&issueIds=${session.compressedEntityId?.urlencoded}"
            } else {
                logger.debug("No persistent id found for ${session.issue.id}, command suggests may be imprecise and slow")
                "$result&query=${session.issue.id}"
            }
        }
}