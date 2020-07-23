package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import com.github.jk1.ytplugin.setupWindow.SetupTask
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.PutMethod

interface SetupManagerTrait : RestClientTrait, YouTrackConnectionTrait {

    override val httpClient: HttpClient
        get() {
            val client = HttpClient()
            client.params.connectionManagerTimeout = 30000 // ms
            client.params.soTimeout = 30000 // ms
            client.params.credentialCharset = "UTF-8"
            client.params.isAuthenticationPreemptive = true
            val credentials = UsernamePasswordCredentials(username, password)
            client.state.setCredentials(AuthScope.ANY, credentials)
            return client
        }


    fun testTestConnection(): String {

        val method = PostMethod("$serverUrl/api/token")
        method.setRequestHeader("Authorization", "Bearer $password")

        val setupTask = SetupTask()
        setupTask.fixURI(method)

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status != 403) {
                method.getResponseHeader("Location").toExternalForm().split("/").last().trim()
            } else {
                throw IllegalStateException("Unable to login: user banned")
            }
        }
    }

}