package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.components.ComponentAware
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.PostMethod


abstract  class AbstractRestClient(override val project: Project) : ComponentAware {

    protected fun createHttpClient(): HttpClient {
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
}