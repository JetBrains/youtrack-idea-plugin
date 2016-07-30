package com.github.jk1.ytplugin.common

import com.intellij.tasks.impl.BaseRepository
import com.intellij.tasks.impl.BaseRepositoryImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.commons.httpclient.HttpClient


class YouTrackServer(private val delegate: BaseRepository) {

    val url: String get() = delegate.url
    val username: String get() = delegate.username
    val password: String get() = delegate.password

    var defaultSearch: String
        get() = (delegate as YouTrackRepository).defaultSearch
        set(value) {
            (delegate as YouTrackRepository).defaultSearch = value
        }

    fun getRestClient(): HttpClient {
        // dirty hack to get preconfigured http client from task management plugin
        // we don't want to handle all the connection/testing/proxy stuff ourselves
        val method = BaseRepositoryImpl::class.java.getDeclaredMethod("getHttpClient")
        method.isAccessible = true
        return method.invoke(delegate) as HttpClient
    }

    fun findTask(id: String) = delegate.findTask(id)

    fun getTasks(query: String?, offset: Int, limit: Int) = delegate.getIssues(query, offset, limit, true)
}