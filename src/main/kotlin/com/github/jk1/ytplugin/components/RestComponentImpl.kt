package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandExecutionResult
import com.github.jk1.ytplugin.model.CommandParseResult
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.PostMethod
import org.jdom.input.SAXBuilder
import java.net.URLEncoder


class RestComponentImpl(override val project: Project) :
        AbstractProjectComponent(project), RestComponent, ComponentAware {

    override fun parseCommand(command: YouTrackCommand): CommandParseResult {
        throw UnsupportedOperationException("Parse command")
    }

    override fun executeCommand(command: YouTrackCommand): CommandExecutionResult {
        doREST(command.executeCommandUrl)
        return CommandExecutionResult() // todo: parse response & fill result
    }

    override fun getUserGroups(login: String): List<String> {
        return listOf("All Users")
    }

    private fun getHttpClient(): HttpClient {
        val repo = taskManagerComponent.getYouTrackRepository()
        val client = taskManagerComponent.getRestClient()
        client.state.setCredentials(AuthScope.ANY,
                UsernamePasswordCredentials(repo.username, repo.password))
        val method = PostMethod("${repo.url}/rest/user/login")
        method.addParameter("login", repo.username)
        method.addParameter("password", repo.password)
        client.params.contentCharset = "UTF-8"
        client.executeMethod(method)
        var response: String?
        try {
            if (method.statusCode != 200) {
                throw Exception("Cannot login: HTTP status code " + method.statusCode)
            }
            response = method.getResponseBodyAsString(1000)
        } finally {
            method.releaseConnection()
        }

        if (response == null) {
            throw NullPointerException()
        } else if (!response.contains("<login>ok</login>")) {
            val pos = response.indexOf("</error>")
            val length = "<error>".length
            if (pos > length) {
                response = response.substring(length, pos)
            }

            throw Exception("Cannot login: " + response)
        } else {
            return client
        }
    }

    fun doREST(url: String) {
        val client = getHttpClient()
        val method = PostMethod(url)
        val status = client.executeMethod(method)
        if (status == 400) {
            val string = method.responseBodyAsStream
            val element = SAXBuilder(false).build(string).rootElement
            // LOG.debug("\n" + JDOMUtil.createOutputter("\n").outputString(JDOMUtil.loadDocument(element)))
            if ("error".equals(element.name)) {
                throw Exception(element.text)
            }
        }
        method.releaseConnection()
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