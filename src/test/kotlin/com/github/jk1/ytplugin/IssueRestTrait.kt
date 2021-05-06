package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpPost
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpContext
import java.nio.charset.StandardCharsets
import java.util.*

interface IssueRestTrait : RestClientTrait, YouTrackConnectionTrait {

    override val httpClient: HttpClient
        get() {
            // todo: migrate towards the common factory
            val socketConfig = SocketConfig.custom().setSoTimeout(30000).build() // ms
            return HttpClientBuilder.create()
                    .setDefaultSocketConfig(socketConfig)
                    .addInterceptorFirst { request: HttpRequest, _: HttpContext ->
                        request.setHeader("Accept", "application/json")
                        request.setHeader("Authorization", "Basic ${"${repository.username}:${repository.password}".b64Encoded}")
                    }
                    .build()
        }

    private val String.b64Encoded: String
        get() = Base64.getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))

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