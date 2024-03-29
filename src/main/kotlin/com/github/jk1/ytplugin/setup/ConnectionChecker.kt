package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import java.nio.charset.StandardCharsets
import java.util.*


class ConnectionChecker(val repository: YouTrackRepository, project: Project) {

    private var onSuccess: (method: HttpRequest) -> Unit = {}
    private var onVersionError: (method: HttpRequest) -> Unit = {}
    private var onTransportError: (request: HttpRequest, e: Exception) -> Unit = { _: HttpRequest, _: Exception -> }
    private var onRedirectionError: (request: HttpRequest, response: HttpResponse) -> Unit = { _: HttpRequest, _: HttpResponse -> }

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
            val client = SetupRepositoryConnector.setupHttpClient(repository)
            val response = client.execute(method)
            if (response.statusLine.statusCode == HttpStatus.SC_OK) {
                if (!credentialsChecker.isGuestUser(response.entity)){
                    logger.debug("connection status: SUCCESS")
                    method.releaseConnection()
                    onSuccess(method)
                } else {
                    logger.debug("connection status: VERSION ERROR")
                    method.releaseConnection()
                    onVersionError(method)
                }
            } else {
                logger.debug("connection status: APPLICATION ERROR")
                method.releaseConnection()
                onRedirectionError(method, response)
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

    fun onRedirectionError(closure: (request: HttpRequest, httpResponse: HttpResponse) -> Unit) {
        this.onRedirectionError = closure
    }

    fun onTransportError(closure: (request: HttpRequest, e: Exception) -> Unit) {
        this.onTransportError = closure
    }

    fun onVersionError(closure: (method: HttpRequest) -> Unit) {
        this.onVersionError = closure
    }
}