package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets
import java.util.*

import org.apache.http.client.config.RequestConfig


class ConnectionChecker(val repository: YouTrackRepository, project: Project) {

    private var onSuccess: (method: HttpRequest) -> Unit = {}
    private var onTransportError: (request: HttpRequest, e: Exception) -> Unit = { _: HttpRequest, _: Exception -> }
    private var onApplicationError: (request: HttpRequest, response: HttpResponse) -> Unit = { _: HttpRequest, _: HttpResponse -> }

    private val credentialsChecker = ComponentAware.of(project).credentialsCheckerComponent

    private val String.b64Encoded: String
        get() = Base64.getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))


    fun check() {
        logger.debug("CHECK CONNECTION FOR ${repository.url}")
        val method = HttpGet(repository.url.trimEnd('/') + "/api/users/me?fields=name")
        if (credentialsChecker.isMatchingAppPassword(repository.password) &&
            !(credentialsChecker.isMatchingBearerToken(repository.password))) {
            repository.username = repository.password.split(Regex(":"), 2).first()
            repository.password = repository.password.split(Regex(":"), 2).last()
        }
        val authCredentials = "${repository.username}:${repository.password}".b64Encoded
        method.setHeader("Authorization", "Basic $authCredentials")

        try {
            val config = RequestConfig.custom().setConnectTimeout(60000).build()
            val response = HttpClientBuilder.create()
                .disableRedirectHandling()
                .setDefaultRequestConfig(config).build()
                .execute(method)
            if (response.statusLine.statusCode == 200) {
                val user = JsonParser.parseString(EntityUtils.toString(response.entity, "UTF-8"))
                    .asJsonObject.get("name").toString()
                if (user != "\"guest\""){
                    logger.debug("connection status: SUCCESS")
                    method.releaseConnection()
                    onSuccess(method)
                }
            } else {
                logger.debug("connection status: APPLICATION ERROR")
                method.releaseConnection()
                onApplicationError(method, response)
            }
        } catch (e: Exception) {
            logger.debug("connection status: TRANSPORT ERROR")
            method.releaseConnection()
            onTransportError(method, e)
        }
    }

    fun onSuccess(closure: (method: HttpRequest) -> Unit) {
        this.onSuccess = closure
    }

    fun onApplicationError(closure: (request: HttpRequest, httpResponse: HttpResponse) -> Unit) {
        this.onApplicationError = closure
    }

    fun onTransportError(closure: (request: HttpRequest, e: Exception) -> Unit) {
        this.onTransportError = closure
    }
}