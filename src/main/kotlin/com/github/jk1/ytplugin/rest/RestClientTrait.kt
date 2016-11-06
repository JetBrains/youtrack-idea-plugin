package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import java.net.URLEncoder


interface RestClientTrait : ComponentAware {

    @Deprecated("Use #createHttpClient(repository) instead")
    fun createHttpClient(): HttpClient {
        return createHttpClient(taskManagerComponent.getActiveYouTrackRepository())
    }

    fun createHttpClient(repository: YouTrackServer): HttpClient {
        val client = repository.getRestClient()
        val credentials = UsernamePasswordCredentials(repository.username, repository.password)
        client.state.setCredentials(AuthScope.ANY, credentials)
        client.params.soTimeout = 10000
        return client
    }

    fun <R> connect(closeable: HttpMethod, block: (HttpMethod) -> R): R {
        try {
            return block(closeable)
        } finally {
            closeable.releaseConnection()
        }
    }

    val String.urlencoded: String
        get() = URLEncoder.encode(this, "UTF-8")
}