package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import java.io.InputStreamReader
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*


interface RestClientTrait: ResponseLoggerTrait {

    val repository: YouTrackServer

    val httpClient: HttpClient
        get() {
            val client = repository.getRestClient()
            val credentials = UsernamePasswordCredentials(repository.username, repository.password)
            client.state.setCredentials(AuthScope.ANY, credentials)
            client.params.soTimeout = 15000
            return client
        }

    fun <R> HttpMethod.connect(block: (HttpMethod) -> R): R {
        try {
            val credentials = "${repository.username}:${repository.password}".b64Encoded
            this.addRequestHeader("Authorization", "Basic $credentials")
            this.addRequestHeader("User-Agent", "YouTrack IDE Plugin")
            return block(this)
        } finally {
            this.releaseConnection()
        }
    }

    fun <T> HttpMethod.execute(responseParser: (json: JsonElement) -> T): T {
        this.setRequestHeader("Accept", "application/json")
        return connect {
            val status = httpClient.executeMethod(this)
            if (status == 200) {
                val stream = InputStreamReader(this.responseBodyAsLoggedStream(), "UTF-8")
                responseParser.invoke(JsonParser().parse(stream))
            } else {
                throw RuntimeException(this.responseBodyAsLoggedString())
            }
        }
    }

    val String.urlencoded: String
        get() = URLEncoder.encode(this, "UTF-8")

    private val String.b64Encoded: String
        get() = Base64.getEncoder().encodeToString(this.toByteArray(Charset.forName("UTF-8")))
}