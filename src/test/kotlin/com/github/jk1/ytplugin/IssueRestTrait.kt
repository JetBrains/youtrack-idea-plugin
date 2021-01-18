package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import com.google.gson.JsonParser
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity

interface IssueRestTrait : RestClientTrait, YouTrackConnectionTrait {

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

    fun createIssue(summary: String = "summary"): String {
        val method = PostMethod("$serverUrl/api/issues?fields=idReadable")
        val body = """{
         "summary": "$summary",
         "project": {
              "id": "81-1"
          }
        }"""
        method.requestEntity = StringRequestEntity(body, "application/json", "UTF-8")
        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                JsonParser.parseString(method.responseBodyAsString).asJsonObject.get("idReadable").asString
            } else {
                throw IllegalStateException("Unable to create issue: ${method.responseBodyAsString}")
            }
        }
    }

    fun touchIssue(id: String) {
        val method = PostMethod("$serverUrl/api/issues/$id")
        val body = """{
          "summary": "Updated summary"
        }"""

        method.requestEntity = StringRequestEntity(body, "application/json", "UTF-8")

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status != 200) {
                throw IllegalStateException("Unable to update an issue: ${method.responseBodyAsString}")
            }
        }
    }

    fun deleteIssue(id: String) {
        val method = DeleteMethod("$serverUrl/api/issues/$id")
        method.connect {
            val status = httpClient.executeMethod(method)
            if (status != 200 && status != 404) {
                throw IllegalStateException("Unable to delete issue: ${method.responseBodyAsString}")
            }
        }
    }
}