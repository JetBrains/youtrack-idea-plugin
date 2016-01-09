package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.PostMethod
import org.jdom.input.SAXBuilder
import java.net.URLEncoder

class CommandRestClient(override val project: Project) : AbstractRestClient(project) {

    /*fun getIntellisense(command: YouTrackCommand): CommandParseResult {
        val method = PostMethod(command.intellisenseCommandUrl)
        try {
            val status = createHttpClient().executeMethod(method)
            if (status != 200) {
                val string = method.responseBodyAsString
                return CommandParseResult() // todo: with error
            }
            return CommandParseResult()
        } finally {
            method.releaseConnection()
        }
    }*/

    fun executeCommand(command: YouTrackCommand): CommandExecutionResult {
        val method = PostMethod(command.executeCommandUrl)
        try {
            val status = createHttpClient().executeMethod(method)
            if (status == 400) {
                val string = method.responseBodyAsStream
                val element = SAXBuilder(false).build(string).rootElement
                // LOG.debug("\n" + JDOMUtil.createOutputter("\n").outputString(JDOMUtil.loadDocument(element)))
                if ("error".equals(element.name)) {
                    return CommandExecutionResult(errors = listOf(element.text))
                } else {
                    return CommandExecutionResult(messages = listOf(element.text))
                }
            }
            return CommandExecutionResult()
        } finally {
            method.releaseConnection()
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
            return "$baseUrl/rest/issue/execute/intellisense/${issues.first().id}?command=$encodedCommand&caret=$caret"
        }

    private val YouTrackCommand.encodedCommand: String
        get() = URLEncoder.encode(command, "UTF-8")
}