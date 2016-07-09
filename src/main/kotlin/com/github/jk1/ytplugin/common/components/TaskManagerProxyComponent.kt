package com.github.jk1.ytplugin.common.components

import com.intellij.openapi.components.ProjectComponent
import com.intellij.tasks.Task
import com.intellij.tasks.impl.BaseRepository
import org.apache.commons.httpclient.HttpClient


interface TaskManagerProxyComponent : ProjectComponent {

    fun getActiveTask(): Task

    fun getActiveYouTrackRepository(): BaseRepository

    fun getAllConfiguredYouTrackRepositories(): List<BaseRepository>

    fun getRestClient() : HttpClient
}