package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.jdom.input.SAXBuilder

class CommandRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun assistCommand(command: YouTrackCommand): CommandAssistResponse {
        val method = GetMethod(command.intellisenseCommandUrl)
        return method.connect {
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
            val status = httpClient.executeMethod(method)
            if (status != 200) {
                val string = method.responseBodyAsLoggedStream()
                val element = SAXBuilder(false).build(string).rootElement
                if ("error" == element.name) {
                    CommandExecutionResponse(errors = listOf(element.text))
                } else {
                    CommandExecutionResponse(messages = listOf(element.text))
                }
            } else {
                method.responseBodyAsLoggedString()
                CommandExecutionResponse()
            }
        }
    }

    private val YouTrackCommandExecution.executeCommandUrl: String
        get () {
            with(command) {
                val execUrl = "${repository.url}/rest/issue/execute/${session.issue.id}"
                var params = "command=${command.urlencoded}&comment=${comment?.urlencoded}&disableNotifications=$silent"
                if (commentVisibleGroup != "All Users") {
                    // 'All Users' shouldn't be passed as a parameter value. Localized YouTracks can't understand that.
                    params = "$params&group=${commentVisibleGroup.urlencoded}"
                }
                return "$execUrl?$params"
            }
        }

    private val YouTrackCommand.intellisenseCommandUrl: String
        get () {
            val assistUrl = "${repository.url}/rest/command/underlineAndSuggestAndCommands"
            val result = "$assistUrl?command=${command.urlencoded}&caret=$caret&noIssuesContext=false"
            return if (session.hasEntityId()) {
                "$result&issueIds=${session.compressedEntityId?.urlencoded}"
            } else {
                logger.debug("No persistent id found for ${session.issue.id}, command suggests may be imprecise and slow")
                "$result&query=${session.issue.id}"
            }
        }
}