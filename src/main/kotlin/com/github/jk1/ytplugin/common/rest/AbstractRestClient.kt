package com.github.jk1.ytplugin.common.rest

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope


abstract  class AbstractRestClient(override val project: Project) : ComponentAware {

    protected fun createHttpClient(): HttpClient {
        val repo = taskManagerComponent.getActiveYouTrackRepository()
        val client = taskManagerComponent.getRestClient()
        client.state.setCredentials(AuthScope.ANY,
                UsernamePasswordCredentials(repo.username, repo.password))
        return client
    }
}