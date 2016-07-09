package com.github.jk1.ytplugin.common.rest

import com.github.jk1.ytplugin.common.components.ComponentAware
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope


interface RestClientTrait : ComponentAware {

    fun createHttpClient(): HttpClient {
        val repo = taskManagerComponent.getActiveYouTrackRepository()
        val client = taskManagerComponent.getRestClient()
        val credentials = UsernamePasswordCredentials(repo.username, repo.password)
        client.state.setCredentials(AuthScope.ANY, credentials)
        return client
    }

    fun <R> connect(closeable: HttpMethod, block: (HttpMethod) -> R): R {
        try {
            return block(closeable);
        } finally {
            closeable.releaseConnection()
        }
    }
}