package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.intellij.tasks.impl.httpclient.NewBaseRepositoryImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost


class ConnectionChecker(val repository: YouTrackRepository) {

    private var onSuccess: (method: HttpRequest) -> Unit = {}

    private var onApplicationError: (request: HttpRequest, response: HttpResponse) -> Unit = { _: HttpRequest, _: HttpResponse -> }

    private var onTransportError: (request: HttpRequest, e: Exception) -> Unit = { _: HttpRequest, _: Exception -> }

    fun check() {
        logger.debug("CHECK CONNECTION FOR ${repository.url}")
        val method = HttpPost(repository.url.trimEnd('/') + "/api/token")
        method.setHeader("Authorization", "Bearer " + repository.password)
        try {
            // dirty hack to get preconfigured http client from task management plugin
            // we don't want to handle all the connection/testing/proxy stuff ourselves
            val function = NewBaseRepositoryImpl::class.java.getDeclaredMethod("getHttpClient")
            function.isAccessible = true
            val response = (function.invoke(repository) as HttpClient).execute(method)
            if (response.statusLine.statusCode == 200) {
                logger.debug("connection status: SUCCESS")
                method.releaseConnection()
                onSuccess(method)
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