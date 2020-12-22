package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.PostMethod

class ConnectionChecker(val repository: YouTrackRepository) {

    private var onSuccess: (method: HttpMethod) -> Unit = {}

    private var onApplicationError: (method: HttpMethod, responseCode: Int) -> Unit = { _: HttpMethod, _: Int -> }

    private var onTransportError: (method: HttpMethod, e: Exception) -> Unit = { _: HttpMethod, _: Exception -> }

    fun check() {
        logger.debug("CHECK CONNECTION FOR ${repository.url}")
        val method = PostMethod(repository.url.trimEnd('/') + "/api/token")
        method.setRequestHeader("Authorization", "Bearer " + repository.password)
        try {
            repository.httpClient.executeMethod(method)
            if (method.statusCode == 200) {
                logger.debug("connection status: SUCCESS")
                method.releaseConnection()
                onSuccess(method)
            } else {
                logger.debug("connection status: APPLICATION ERROR")
                method.releaseConnection()
                onApplicationError(method, method.statusCode)
            }
        } catch (e: Exception) {
            logger.debug("connection status: TRANSPORT ERROR")
            method.releaseConnection()
            onTransportError(method, e)
        }
    }

    fun onSuccess(closure: (method: HttpMethod) -> Unit) {
        this.onSuccess = closure
    }

    fun onApplicationError(closure: (method: HttpMethod, responseCode: Int) -> Unit) {
        this.onApplicationError = closure
    }

    fun onTransportError(closure: (method: HttpMethod, e: Exception) -> Unit) {
        this.onTransportError = closure
    }
}