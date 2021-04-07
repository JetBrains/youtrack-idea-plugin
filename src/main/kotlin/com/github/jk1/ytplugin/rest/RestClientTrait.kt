package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import java.io.InputStreamReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*


interface RestClientTrait : ResponseLoggerTrait {

    val String.jsonEntity
        get() = StringEntity(this, ContentType.create(ContentType.APPLICATION_JSON.mimeType, StandardCharsets.UTF_8))

    val repository: YouTrackServer

    val httpClient: HttpClient
        get() = repository.getRestClient()

    fun HttpUriRequest.execute(): Unit = execute { }

    fun <T> HttpUriRequest.execute(responseParser: (json: JsonElement) -> T): T {
        this.setHeader("Accept", "application/json")
        this.addHeader("Authorization", "Basic ${"${repository.username}:${repository.password}".b64Encoded}")
        this.addHeader("User-Agent", "YouTrack IDE Plugin")
        val response = httpClient.execute(this)
        try {
            if (response.statusLine.statusCode == 200) {
                val streamReader = InputStreamReader(response.responseBodyAsLoggedStream(), StandardCharsets.UTF_8.name())
                return responseParser.invoke(JsonParser.parseReader(streamReader))
            } else {
                throw RuntimeException(response.responseBodyAsLoggedString())
            }
        } finally {
            // closes the connection
            EntityUtils.consume(response.entity)
        }
    }

    val HttpResponse.responseBodyAsReader
        get() = InputStreamReader(entity.content, StandardCharsets.UTF_8)

    val String.urlencoded: String
        get() = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private val String.b64Encoded: String
        get() = Base64.getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))
}