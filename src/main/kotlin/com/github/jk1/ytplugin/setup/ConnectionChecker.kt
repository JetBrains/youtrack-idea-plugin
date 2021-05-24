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
import org.apache.http.conn.ConnectTimeoutException
import java.net.UnknownHostException


class ConnectionChecker(val repository: YouTrackRepository, project: Project) {

    private var onSuccess: (method: HttpRequest) -> Unit = {}

    private var onInputError: (request: HttpRequest) -> Unit = { _: HttpRequest -> }

    private var onTransportError: (request: HttpRequest, e: Exception) -> Unit = { _: HttpRequest, _: Exception -> }
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
            // todo: proxy
            //timeout is required to handle connections with non-existing url looking like existing
            val config = RequestConfig.custom().setConnectTimeout(10000).build()
            val response = HttpClientBuilder.create().disableRedirectHandling().setDefaultRequestConfig(config).build()
                .execute(method)
            val user = JsonParser.parseString(EntityUtils.toString(response.entity, "UTF-8")).asJsonObject.get("name")
                .toString()
            if (response.statusLine.statusCode == 200 && user != "\"guest\"") {
                logger.debug("connection status: SUCCESS")
                method.releaseConnection()
                onSuccess(method)
            }

        } catch (e: ConnectTimeoutException) {
            logger.debug("connection status: TIMEOUT ERROR")
            method.releaseConnection()
            onInputError(method)
        } catch (e: UnknownHostException) {
            logger.debug("connection status: HOST ERROR")
            method.releaseConnection()
            onInputError(method)
        } catch (e: Exception) {
            logger.debug("connection status: TRANSPORT ERROR")
            method.releaseConnection()
            onTransportError(method, e)
        }
    }

    fun onSuccess(closure: (method: HttpRequest) -> Unit) {
        this.onSuccess = closure
    }

    fun onInputError(closure: (request: HttpRequest) -> Unit) {
        this.onInputError = closure
    }

    fun onTransportError(closure: (request: HttpRequest, e: Exception) -> Unit) {
        this.onTransportError = closure
    }
}