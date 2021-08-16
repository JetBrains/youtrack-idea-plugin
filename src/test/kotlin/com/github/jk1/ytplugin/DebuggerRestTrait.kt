package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpContext
import java.nio.charset.StandardCharsets
import java.util.*

interface DebuggerRestTrait : RestClientTrait, YouTrackConnectionTrait {

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

    fun getWsUrl(url: String = serverUrl): String? {
        val request = HttpGet("$url/api/debug/scripts/json")
        return try {
            request.execute { it.asJsonArray[0].asJsonObject.get("webSocketDebuggerUrl").asString }
        } catch (e: RuntimeException){
            null
        } catch (e: Exception){
            logger.info("Exception: ${e.message}")
            null
        }
    }
}