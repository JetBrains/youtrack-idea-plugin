package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpPost
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder

interface IssueRestTrait : RestClientTrait, YouTrackConnectionTrait {

    override val httpClient: HttpClient
        get() {
            val credentialsProvider = BasicCredentialsProvider()
            credentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
            val socketConfig = SocketConfig.custom().setSoTimeout(30000).build() // ms
            return HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setDefaultSocketConfig(socketConfig)
                    .build()
        }

    fun createIssue(summary: String = "summary"): String {
        val request = HttpPost("$serverUrl/api/issues?fields=idReadable")
        val body = """{
         "summary": "$summary",
         "project": {
              "id": "81-1"
          }
        }"""
        request.entity = body.jsonEntity
        return request.execute { it.asJsonObject.get("idReadable").asString }
    }

    fun touchIssue(id: String) {
        val method = HttpPost("$serverUrl/api/issues/$id")
        val body = """{
          "summary": "Updated summary"
        }"""
        method.entity = body.jsonEntity
        return method.execute()
    }

    fun deleteIssue(id: String) {
        HttpDelete("$serverUrl/api/issues/$id").execute()
    }
}