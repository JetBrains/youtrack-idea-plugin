package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.PostMethod

class ConnectionChecker(val repository: YouTrackRepository) {
    @Volatile
    private var onSuccess: () -> Unit = {}

    @Volatile
    private var onApplicationError: (method: HttpMethod, responseCode: Int) -> Unit = { _: HttpMethod, _: Int -> }

    @Volatile
    private var onTransportError: (method: HttpMethod, e: Exception) -> Unit = { _: HttpMethod, _: Exception -> }

    fun check() {
        // TODO: better way of checking
        if (Thread.currentThread().name.contains("pooled")) {
            doConnect()
        } else {
            ApplicationManager.getApplication().executeOnPooledThread {
                doConnect()
            }
        }
    }

    private fun doConnect() {
        logger.debug("CHECK CONNECTION FOR ${repository.url}")

        val method = PostMethod(repository.url.trimEnd('/') + "/api/token")
        method.setRequestHeader("Authorization", "Bearer " + repository.password)
        try {
            repository.httpClient.executeMethod(method)
            if (method.statusCode == 200) {
                logger.debug("connection status: SUCCESS")
                onSuccess()
            } else {
                logger.debug("connection status: APPLICATION ERROR")
                onApplicationError(method, method.statusCode)
            }
        } catch (e: Exception) {
            logger.debug("connection status: TRANSPORT ERROR")
            onTransportError(method, e)
        } finally {
            logger.debug("connection status: RELEASED")
            method.releaseConnection()
        }
    }

    fun onSuccess(closure: () -> Unit) {
        this.onSuccess = {
            ApplicationManager.getApplication().invokeLater {
                closure.invoke()
            }
        }
    }

    fun onApplicationError(closure: (method: HttpMethod, responseCode: Int) -> Unit) {
        this.onApplicationError = { method: HttpMethod, responseCode: Int ->
            ApplicationManager.getApplication().invokeLater {
                closure.invoke(method, responseCode)
            }
        }
    }

    fun onTransportError(closure: (method: HttpMethod, e: Exception) -> Unit) {
        this.onTransportError = { method: HttpMethod, e: Exception ->
            ApplicationManager.getApplication().invokeLater {
                closure.invoke(method, e)
            }
        }
    }
}